package com.tlvlp.ddns.tls.service;

import com.tlvlp.ddns.tls.service.records.DnsRecord;
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

    private DnsRecord getReferenceRecord() {
        return new DnsRecord()
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
        DnsRecord dnsRecord = getReferenceRecord();

        // when
        registrarHandler.replaceRecordContent(dnsRecord, "updated");

        // then
        String recordContent = registrarHandler.getRecordContent(dnsRecord);
        assertThat(recordContent).isEqualTo("updated");
    }

    @Test
    public void getRecordContentTest() {
        //given
        RegistrarHandler registrarHandler = Registrars.GODADDY.getHandler();
        DnsRecord dnsRecord = getReferenceRecord();

        // when
        String recordContent = registrarHandler.getRecordContent(dnsRecord);

        // then - lo and behold the console output!
        System.out.println("contents: " + recordContent);
    }



}