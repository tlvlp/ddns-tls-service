package com.tlvlp.ddns.tls.service.tls;

import com.tlvlp.ddns.tls.service.records.ConfigService;
import com.tlvlp.ddns.tls.service.records.DnsRecordTLS;
import com.tlvlp.ddns.tls.service.records.DnsUpdaterService;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.toolbox.AcmeUtils;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
        if(isCheckRunning) {
            log.info("Previous check is still running.");
            return;
        }
        if(isServiceActive) {
            publisher.publishEvent(new TlsCertCheckRequiredEvent(this));
        }
    }

    @EventListener(TlsCertCheckRequiredEvent.class)
    public void runTlsCertCheckAndUpdate() {
        if(!isServiceActive) {
            return;
        }
        log.info("Running scheduled TLS certificate update.");
        isCheckRunning = true;
        configService.getTlsRecords().forEach(this::tlsCertUpdate);
        isCheckRunning = false;
    }

    private void tlsCertUpdate(DnsRecordTLS record) {
        try {
            log.info("Updating certificate(s) for record: {}", record);
            KeyPair userKeyPair = readOrGenerateKeyPair(record.getUserKeyPairRef(), configFolderPath);
            KeyPair domainKeyPair = readOrGenerateKeyPair(record.getDomainKeyPairRef(), certFolderPath);
            Session letsEncryptSession = new Session(certProviderUrl);
            Account account = findOrRegisterAccount(letsEncryptSession, userKeyPair);
            log.info("Creating and saving a signing request.");
            CSRBuilder csr = new CSRBuilder();
            csr.addDomains(record.getDomainsToCover());
            csr.sign(domainKeyPair);
            saveCsrToFile(csr, record.getDomain());
            log.info("Ordering certificates");
            Certificate cert = orderCertificate(account, csr, record);
            log.info("Certificate generated for domains {} (url: {})", record.getDomainsToCover(), cert.getLocation());
            saveCertificatesToFile(cert, record);
            saveP12KeysStoreToFile(cert, record, domainKeyPair);
            updateCertPermissions();
        } catch (Exception e) {
            log.error("Error while running TLS certificate check and update for domain " + record.getDomain(), e);
        }
    }

    private KeyPair readOrGenerateKeyPair(String keypairSourceRef, String folderPath) {
        try {
            String keyPairString = checkForEnvVariable(keypairSourceRef);
            if(keyPairString == null) {
                keyPairString = checkForFileBasedKey(keypairSourceRef, folderPath);
            }
            if(keyPairString == null) {
                return generateAndSaveKeyPair(keypairSourceRef, folderPath);
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

    private KeyPair generateAndSaveKeyPair(String caAuthKeyPairRef, String folderPath) {
        log.info("Generating new CA authentication key pair");
        KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
        try (FileWriter writer = new FileWriter(String.format("%s/%s", folderPath, caAuthKeyPairRef))) {
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
        log.info("User account URL: {}", account.getLocation());
        return account;
    }

    private void saveCsrToFile(CSRBuilder csr, String domainName) {
        log.info("Saving CSR to file");
        try (Writer out = new FileWriter(String.format("%s/%s.csr", certFolderPath, domainName))) {
            csr.write(out);
        } catch (Exception e) {
            log.error("Failed to save csr file for domain: {}", domainName);
        }
    }

    private Certificate orderCertificate(Account account, CSRBuilder csr, DnsRecordTLS record) {
        try {
            Order order = account.newOrder().domains(record.getDomainsToCover()).create();
            log.info("Obtaining authorization for the domains");
            for (Authorization auth : order.getAuthorizations()) {
                authorize(auth, record);
            }
            log.info("Ordering certificates and waiting for CA (max 30sec)");
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
            String baseDomain = record.getDomain();
            String subDomain = authDomain
                    .replace("*", "")               // remove the wildcard (cert)
                    .replace("." + baseDomain, "")  // remove the base domain that had a subdomain
                    .replace(baseDomain, "");              // remove the base domain (the result should be empty for wildcard certs)
            record
                    .setType("TXT")
                    .setName(String.format("_acme-challenge%s", subDomain.isEmpty() ? "" : "." + subDomain) );
            dnsUpdaterService.checkAndUpdateRecord(record, challenge.getDigest());

            log.info("Waiting for response form the CA (with a 30sec initial delay)");
            Thread.sleep(30 * 1000);
            challenge.trigger();
            int attempts = 10;
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                log.info("Authorization DNS Challenge has failed! Attempts left: {} status: {} error: {}", attempts, challenge.getStatus(), challenge.getError());
                Thread.sleep(3000L);
                challenge.update();
            }

            if (challenge.getStatus() != Status.VALID) {
                throw new RuntimeException("Failed to pass the challenge for domain in 10 attempts:" + authDomain);
            }
            log.info("Authorization DNS Challenge was passed for domain :" + authDomain);
        } catch (Exception e) {
            log.error("Authorization failed!", e);
        }
    }

    private void saveCertificatesToFile(Certificate certificate, DnsRecordTLS record) {
        log.debug("Saving certificates to file");
        String pathBase = String.format("%s/%s", certFolderPath, record.getDomain());

        String certPath = pathBase + ".crt";
        try (FileWriter fw = new FileWriter(certPath)) {
            AcmeUtils.writeToPem(certificate.getCertificate().getEncoded(), AcmeUtils.PemLabel.CERTIFICATE, fw);
            log.info("Certificate saved to: {}", certPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate file for domains: " + record.getDomainsToCover());
        }

        String certChainPath = pathBase + "_chain.crt";
        try (FileWriter fw = new FileWriter(certChainPath)) {
            certificate.writeCertificate(fw);
            log.info("Certificate chain saved to: {}", certChainPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate chain file for domains: " + record.getDomainsToCover());
        }

    }

    private void saveP12KeysStoreToFile(Certificate certificate, DnsRecordTLS record, KeyPair domainKeyPair) {
        log.debug("Generating and saving p12 keystore to file");
        if(record.getKeyStorePass().isEmpty()) {
            throw new RuntimeException("Field keyStorePass cannot be empty in record: " + record);
        }
        String keyStorePath =  String.format("%s/%s_keystore.p12", certFolderPath, record.getDomain());
        try (FileOutputStream p12Writer = new FileOutputStream(keyStorePath)) {
            // Create new keystore
            char[] keystorePass = record.getKeyStorePass().toCharArray();
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(null, keystorePass);

            // Get cert chain
            List<X509Certificate> certChainList = certificate.getCertificateChain();
            X509Certificate[] certChainArray = new X509Certificate[certChainList.size()];
            certChainArray = certChainList.toArray(certChainArray);

            // Save cert chain in keystore
            pkcs12.setKeyEntry("cert", domainKeyPair.getPrivate(), keystorePass, certChainArray);

            // Write keystore to file
            pkcs12.store(p12Writer, keystorePass);
            log.info("P12 Keystore generated and saved to: {}", keyStorePath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and save keystore file for domains: " + record.getDomainsToCover());
        }

    }

    private void updateCertPermissions() {
        try(Stream<Path> pathStream = Files.walk(Path.of(certFolderPath),2)) {
            pathStream.forEach(path -> {
                try {
                    Files.setPosixFilePermissions(path, Set.of(
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_WRITE,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE
                    ));
                } catch (Exception e) {
                    throw new RuntimeException("At file: " + path, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to update file permissions for the cert folder. ", e);
        }

    }

}
