package com.tlvlp.ddns.tls.service.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlvlp.ddns.tls.service.registrars.Record;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecordService {
    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final ConfigurableEnvironment environment;
    private final Path recordsFilePath;
    private String recordsFileHash;
    private Set<Record> ddnsRecords;
    private final ApplicationEventPublisher publisher;

    public RecordService(ConfigurableEnvironment environment, ApplicationEventPublisher publisher) {
        this.environment = environment;
        this.recordsFilePath = Path.of(environment.getProperty("records.file.path"));
        this.publisher = publisher;
    }

    @Scheduled(cron = "${record.update.schedule}")
    public void checkRecordFileChanges() {
        try {
            log.debug("Checking record file changes");
            if(!Files.isReadable(recordsFilePath)) {
                throw new RuntimeException("File cannot be read: " + recordsFilePath);
            }
            String currentHash = DigestUtils.md5Hex(Files.newInputStream(recordsFilePath)).toUpperCase();
            if(!currentHash.equals(recordsFileHash)) {
                log.info("File change detected - Updating records");
                ddnsRecords = updatedRecordsFromFile(Files.newInputStream(recordsFilePath));
                recordsFileHash = currentHash;
                log.info("Updated records:");
                ddnsRecords.forEach(record -> log.info(record.toString()));
                publisher.publishEvent(new DnsCheckRequiredEvent(this, true));
            }
        } catch (Exception e) {
            log.error("Unable to update records after file change: ", e);
        }
    }

    private Set<Record> updatedRecordsFromFile(InputStream resourceStream) throws IOException {
        Set<Record> records = new ObjectMapper().readValue(resourceStream, new TypeReference<>() {});
        return updateRecordsWithSecrets(records);

    }

    private Set<Record> updateRecordsWithSecrets(Set<Record> records) {
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

    public Set<Record> getDdnsRecords() {
        LocalTime start = LocalTime.now();
        while (LocalTime.now().isBefore(start.plusSeconds(5))) {
            if(ddnsRecords != null) {
                return new HashSet<>(ddnsRecords);
            }
        }
        throw new RuntimeException("5s Timeout reached to get DDNS Records!");
    }

}


