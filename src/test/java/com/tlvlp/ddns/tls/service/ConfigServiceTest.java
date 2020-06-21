package com.tlvlp.ddns.tls.service;

import com.tlvlp.ddns.tls.service.records.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
class ConfigServiceTest {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SecretsLoaderMock secretsLoaderMock;

    private final Map<String, Object> secretMap = Map.of(
            "myapikey1", "mysecret1",
            "myapikey2", "mysecret2"
    );

    @BeforeEach
    public void init() {
        secretsLoaderMock.addProperties(secretMap);
    }

    @Test
    void getDdnsRecordsTest() {
        //given a record service that has been initialized at startup with:
        // - Records read from a json file
        // - Records updated with apiSecrets from environment variables
        configService.scheduledCheckRecordFileChanges();

        //when
        var records = configService.getDdnsRecords();

        //then
        assertThat(records)
                .as("Record secrets are populated")
                .isNotEmpty()
                .allMatch(record -> record.getApiSecret() != null);

        records.forEach(record -> assertThat(record.getApiSecret())
                .isEqualTo(secretMap.get(record.getApiKey())));

    }

}