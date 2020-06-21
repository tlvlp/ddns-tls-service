package com.tlvlp.ddns.tls.service.records;

import com.tlvlp.ddns.tls.service.registrars.RegistrarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DnsUpdaterService {
    private static final Logger log = LoggerFactory.getLogger(DnsUpdaterService.class);

    public void checkAndUpdateRecord(DnsRecord dnsRecord, String newValue) {
        try {
            RegistrarHandler registrarHandler = dnsRecord.getRegistrar().getHandler();
            String valueAtRegistrar = registrarHandler.getRecordContent(dnsRecord);
            if(newValue.equals(valueAtRegistrar)) {
                log.debug("Existing value matches value at registrar. No update required.");
                return;
            }
            registrarHandler.replaceRecordContent(dnsRecord, newValue);
            log.info(String.format("Record at registrar updated (%s -> %s): %s",valueAtRegistrar, newValue, dnsRecord));
        } catch (Exception e) {
            log.error(String.format("Unable to complete scheduled DDNS check or update for record: %s%n", dnsRecord), e);
        }
    }


}
