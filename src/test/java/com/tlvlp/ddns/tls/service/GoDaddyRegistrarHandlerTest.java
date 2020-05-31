package com.tlvlp.ddns.tls.service;

import com.tlvlp.ddns.tls.service.registrars.Record;
import com.tlvlp.ddns.tls.service.registrars.RegistrarHandler;
import com.tlvlp.ddns.tls.service.registrars.Registrars;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@DisplayName("GoDaddy registrar handler tests (for manual testing - requires valid key/secret pair)")
class GoDaddyRegistrarHandlerTest {

    private final String testApiKey = "XXXX";
    private final String testApiSecret = "XXXX";

    private Record getReferenceRecord() {
        return new Record()
                .setApiKey(testApiKey)
                .setApiSecret(testApiSecret)
                .setRegistrar(Registrars.GODADDY)
                .setDomain("tlvlp.com")
                .setType("TXT")
                .setName("APITEST");
    }

    @Test
    public void replaceRecordContentTest() {
        //given
        RegistrarHandler registrarHandler = Registrars.GODADDY.getHandler();
        Record record = getReferenceRecord();

        // when
        registrarHandler.replaceRecordContent(record, "updated");

        // then
        String recordContent = registrarHandler.getRecordContent(record);
        assertThat(recordContent).isEqualTo("updated");
    }

    @Test
    public void getRecordContentTest() {
        //given
        RegistrarHandler registrarHandler = Registrars.GODADDY.getHandler();
        Record record = getReferenceRecord();

        // when
        String recordContent = registrarHandler.getRecordContent(record);

        // then - lo and behold the console output!
        System.out.println("contents: " + recordContent);
    }



}