package com.tlvlp.ddns.tls.service.tls;

import com.tlvlp.ddns.tls.service.records.ConfigService;
import com.tlvlp.ddns.tls.service.records.DnsRecordTLS;
import com.tlvlp.ddns.tls.service.records.DnsUpdaterService;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;

@Service
public class TlsCertUpdaterService {
    private static final Logger log = LoggerFactory.getLogger(TlsCertUpdaterService.class);

    private final ConfigurableEnvironment environment;
    private final Boolean isServiceActive;
    private final ApplicationEventPublisher publisher;
    private final String certFolderPath;
    private final String configFolderPath;
    private final String certProviderUrl;
    private final ConfigService configService;
    private final DnsUpdaterService dnsUpdaterService;

    private Boolean isCheckRunning;


    public TlsCertUpdaterService(@Value("${config.folder.path}") String configFolderPath,
                                 @Value("${tls.cert.service.active}") Boolean isServiceActive,
                                 @Value("${tls.cert.folder.path}") String certFolderPath,
                                 @Value("${tls.cert.provider.url}") String certProviderUrl,
                                 ApplicationEventPublisher publisher,
                                 ConfigurableEnvironment environment,
                                 ConfigService configService,
                                 DnsUpdaterService dnsUpdaterService) {
        this.isServiceActive = isServiceActive;
        this.publisher = publisher;
        this.certFolderPath = certFolderPath;
        this.environment = environment;
        this.configService = configService;
        this.dnsUpdaterService = dnsUpdaterService;
        this.configFolderPath = configFolderPath;
        this.certProviderUrl = certProviderUrl;
        this.isCheckRunning = false;
    }

    @Scheduled(cron = "${tls.cert.check.schedule}")
    public void scheduledTlsCertCheckAndUpdate() {
        if(isServiceActive) {
            publisher.publishEvent(new TlsCertCheckRequiredEvent(this));
        }
    }

    @EventListener(TlsCertCheckRequiredEvent.class)
    public void runTlsCertCheckAndUpdate() {
        if(!isServiceActive) {
            return;
        }
        if(isCheckRunning) {
            log.info("Previous check is still running.");
            return;
        }
        log.info("Running scheduled TLS certificate update.");
        configService.getTlsRecords().forEach(this::tlsCertCheckAndUpdate);
        isCheckRunning = false;
    }

    private void tlsCertCheckAndUpdate(DnsRecordTLS record) {
        try {
            isCheckRunning = true;
            KeyPair userKeyPair = readOrGenerateKeyPair(record.getUserKeyPairRef());
            KeyPair domainKeyPair = readOrGenerateKeyPair(record.getDomainKeyPairRef());
            Session letsEncryptSession = new Session(certProviderUrl);
            Account account = findOrRegisterAccount(letsEncryptSession, userKeyPair);
            log.debug("Creating and saving signing request.");
            CSRBuilder csr = new CSRBuilder();
            csr.addDomains(record.getDomainsToCover());
            csr.sign(domainKeyPair);
            saveCsrToFile(csr, record.getDomain());
            log.debug("Ordering certificate.");
            Certificate cert = orderCertificate(account, csr, record);
            saveCertificateToFile(cert, record);
        } catch (Exception e) {
            log.error("Error while running TLS certificate check and update for domain " + record.getDomain(), e);
        }
    }

    private KeyPair readOrGenerateKeyPair(String keypairSourceRef) {
        try {
            String keyPairString = checkForEnvVariable(keypairSourceRef);
            if(keyPairString == null) {
                keyPairString = checkForFileBasedKey(keypairSourceRef, configFolderPath);
            }
            if(keyPairString == null) {
                return generateAndSaveKeyPair(keypairSourceRef, configFolderPath);
            }
            return KeyPairUtils.readKeyPair(new StringReader(keyPairString));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get or generate keypair for: %s", keypairSourceRef), e);
        }
    }

    private String checkForEnvVariable(String caAuthKeyPairRef) {
        return environment.getProperty(caAuthKeyPairRef);
    }

    private String checkForFileBasedKey(String caAuthKeyPairRef, String configFolder) {
        try {
            return Files.readString(Path.of(String.format("%s/%s", configFolder, caAuthKeyPairRef)));
        } catch (Exception e) {
            return null;
        }
    }

    private KeyPair generateAndSaveKeyPair(String caAuthKeyPairRef, String configFolder) {
        log.info("Generating new CA authentication key pair");
        KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
        try (FileWriter writer = new FileWriter(String.format("%s/%s", configFolder, caAuthKeyPairRef))) {
            KeyPairUtils.writeKeyPair(keyPair, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save generated CA auth keypair!", e);
        }
        return keyPair;
    }

    private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {
        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(accountKey)
                .agreeToTermsOfService()
                .create(session);
        log.info("Registered a new user, URL: {}", account.getLocation());

        return account;
    }

    private void saveCsrToFile(CSRBuilder csr, String domainName) {
        try (Writer out = new FileWriter(String.format("%s/%s.csr", certFolderPath, domainName))) {
            csr.write(out);
        } catch (Exception e) {
            log.error("Failed to save csr file for domain: " + domainName);
        }
    }

    private Certificate orderCertificate(Account account, CSRBuilder csr, DnsRecordTLS record) {
        try {
            Order order = account.newOrder().domains(record.getDomainsToCover()).create();
            log.info("Obtaining authorization for the domains.");
            for (Authorization auth : order.getAuthorizations()) {
                authorize(auth, record);
            }
            log.info("Ordering certificates and waiting for CA (max 30sec).");
            order.execute(csr.getEncoded());
            LocalDateTime attemptsEndTime = LocalDateTime.now().plusHours(1);
            LocalDateTime lastAttempt = null;
            while (order.getStatus() != Status.VALID && LocalDateTime.now().isBefore(attemptsEndTime)) {
                if (lastAttempt != null && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(1))) {
                    // Waiting between attempts
                    continue;
                }
                if (order.getStatus() == Status.INVALID) {
                    log.error(String.format("Order failed for record: %s %nWill attempt again in every 1 minutes until %s",
                            record, attemptsEndTime));
                }
                if (order.getStatus() == Status.INVALID) {
                    throw new RuntimeException("Order failed.");
                }
                order.update();
                lastAttempt = LocalDateTime.now();
            }
            return order.getCertificate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to order and save certificate.", e);
        }
    }

    private void authorize(Authorization auth, DnsRecordTLS record) {
        try {
            String authDomain = auth.getIdentifier().getDomain();
            log.info("Attempting authorization DNS challenge for domain: " + authDomain);
            if (auth.getStatus() == Status.VALID) {
                return;
            }
            Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
            if (challenge == null) {
                throw new RuntimeException(String.format("No DNS challenge found for the auth connected to record: %s", record));
            }

            // Update DNS record
            record
                    .setType("TXT")
                    .setName("_acme-challenge");
            dnsUpdaterService.checkAndUpdateRecord(record, challenge.getDigest());

            // Notify the CA to check the record and wait for the result
            challenge.trigger();

            log.info("Waiting for response form the CA.");
            LocalDateTime attemptsEndTime = LocalDateTime.now().plusHours(1);
            LocalDateTime lastAttempt = null;
            while (challenge.getStatus() != Status.VALID && LocalDateTime.now().isBefore(attemptsEndTime)) {
                if (lastAttempt != null && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(5))) {
                    // Waiting between attempts
                    continue;
                }
                if (challenge.getStatus() == Status.INVALID) {
                    log.error(String.format("Challenge failed for domain: %s %n%s %nWill attempt again in every 5 minutes until %s",
                            authDomain, challenge.getError(), attemptsEndTime));
                    challenge.trigger();
                }
                challenge.update();
                lastAttempt = LocalDateTime.now();
            }

            if (challenge.getStatus() != Status.VALID) {
                throw new RuntimeException("Failed to pass the challenge for domain :" + authDomain);
            }
            log.info("Authorization DNS Challenge was passed for domain :" + authDomain);
        } catch (Exception e) {
            log.error("Authorization failed!", e);
        }
    }

    private void saveCertificateToFile(Certificate certificate, DnsRecordTLS record) {
        String domainName = record.getDomain();
        String path = String.format("%s/%s_chain.crt", certFolderPath, domainName);
        try (FileWriter fw = new FileWriter(path)) {
            certificate.writeCertificate(fw);
            log.info("Certificate generated and saved for domains {} at path {}", record.getDomainsToCover(), path);
            log.info("Certificate URL: {}", certificate.getLocation());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate file for domain: " + domainName);
        }

    }


}
