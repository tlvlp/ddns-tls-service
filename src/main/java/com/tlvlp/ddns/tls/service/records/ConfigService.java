package com.tlvlp.ddns.tls.service.records;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlvlp.ddns.tls.service.ddns.DnsCheckRequiredEvent;
import com.tlvlp.ddns.tls.service.tls.TlsCertCheckRequiredEvent;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigService {
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigurableEnvironment environment;
    private final Path ddnsRecordsFilePath;
    private final Path tlsRecordsFilePath;
    private final ApplicationEventPublisher publisher;
    private final Boolean isDdnsServiceActive;
    private final Boolean isTlsServiceActive;

    private String ddnsRecordsFileHash;
    private String tlsRecordsFileHash;
    private Set<DnsRecord> ddnsRecords;
    private Set<DnsRecordTLS> tlsDnsRecords;

    public ConfigService(@Value("${config.folder.path}") String configFolderPath,
                         @Value("${ddns.service.active}") Boolean isDdnsServiceActive,
                         @Value("${dns.records.ddns.file}") String ddnsRecordsFilePathString,
                         @Value("${tls.cert.service.active}") Boolean isTlsServiceActive,
                         @Value("${dns.records.tls.file}") String tlsRecordsFilePathString,
                         ConfigurableEnvironment environment,
                         ApplicationEventPublisher publisher) {
        this.environment = environment;
        this.ddnsRecordsFilePath = Path.of(String.format("%s/%s",configFolderPath, ddnsRecordsFilePathString));
        this.tlsRecordsFilePath = Path.of(String.format("%s/%s",configFolderPath, tlsRecordsFilePathString));
        this.publisher = publisher;
        this.isDdnsServiceActive = isDdnsServiceActive;
        this.isTlsServiceActive = isTlsServiceActive;
    }

    @Scheduled(fixedDelayString = "${config.update.delay}")
    public void scheduledCheckRecordFileChanges() {
        if(isDdnsServiceActive) {
           checkRecordFileChangesDdns();
        }
        if(isTlsServiceActive) {
            checkRecordFileChangesTls();
        }
    }

    private void checkRecordFileChangesDdns() {
        try {
            String newFileHash = checkRecordFileForUpdates(ddnsRecordsFilePath);
            if (newFileHash.equals(ddnsRecordsFileHash)) {
                return;
            }
            log.debug("DDNS record file changes detected.");
            ddnsRecords = getDnsRecordsFromFileWithApiSecret(Files.newInputStream(ddnsRecordsFilePath));
            ddnsRecordsFileHash = newFileHash;
            logUpdates(ddnsRecords, ddnsRecordsFilePath.toString());
            publisher.publishEvent(new DnsCheckRequiredEvent(this, true));
        } catch (Exception e) {
            log.error("Unable to check DDNS record file changes.", e);
        }
    }

    private void checkRecordFileChangesTls() {
        try {
            String newFileHash = checkRecordFileForUpdates(tlsRecordsFilePath);
            if (newFileHash.equals(tlsRecordsFileHash)) {
                return;
            }
            log.debug("TLS record file changes detected.");
            tlsDnsRecords = getTlsDnsRecordsFromFileWithApiSecret(Files.newInputStream(tlsRecordsFilePath));
            tlsRecordsFileHash = newFileHash;
            logUpdates(tlsDnsRecords, tlsRecordsFilePath.toString());
            publisher.publishEvent(new TlsCertCheckRequiredEvent(this));
        } catch (Exception e) {
            log.error("Unable to check TLS record file changes.", e);
        }
    }


    private String checkRecordFileForUpdates(Path filePath) throws IOException {
        if (!Files.isReadable(filePath)) {
            throw new RuntimeException("File cannot be read: " + filePath);
        }
        return DigestUtils.md5Hex(Files.newInputStream(filePath)).toUpperCase();
    }

    private Set<DnsRecord> getDnsRecordsFromFileWithApiSecret(InputStream resourceStream) throws IOException {
        Set<DnsRecord> records = new ObjectMapper().readValue(resourceStream, new TypeReference<>() {});
        return records.stream()
                .map(record -> {
                    var apiSecret = environment.getProperty(record.getApiKey());
                    if (apiSecret == null) {
                        throw new RuntimeException("Cannot find api secret for api key: " + record.getApiKey());
                    }
                    return record.setApiSecret(apiSecret);
                })
                .collect(Collectors.toSet());
    }

    private Set<DnsRecordTLS> getTlsDnsRecordsFromFileWithApiSecret(InputStream resourceStream) throws IOException {
        Set<DnsRecordTLS> records = new ObjectMapper().readValue(resourceStream, new TypeReference<>() {});
        return records.stream()
                .map(record -> {
                    var apiSecret = environment.getProperty(record.getApiKey());
                    if (apiSecret == null) {
                        throw new RuntimeException("Cannot find api secret for api key: " + record.getApiKey());
                    }
                    return record.setApiSecret(apiSecret);
                })
                .collect(Collectors.toSet());
    }

    private <T extends DnsRecord> void logUpdates(Collection<T> records, String filePath) {
        StringBuilder updates = new StringBuilder();
        records.forEach(record -> updates.append(String.format("    %s%n",record.toString())));
        log.info(String.format("Updated records (%s): %n%s", filePath, updates));
    }


    public Set<DnsRecord> getDdnsRecords() {
        LocalTime start = LocalTime.now();
        while (LocalTime.now().isBefore(start.plusSeconds(5))) {
            if(ddnsRecords != null) {
                return new HashSet<>(ddnsRecords);
            }
        }
        throw new RuntimeException("5s Timeout reached to get DDNS Records!");
    }

    public Set<DnsRecordTLS> getTlsRecords() {
        LocalTime start = LocalTime.now();
        while (LocalTime.now().isBefore(start.plusSeconds(5))) {
            if(tlsDnsRecords != null) {
                return new HashSet<>(tlsDnsRecords);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
        throw new RuntimeException("5s Timeout reached to get TLS Records!");
    }

}


