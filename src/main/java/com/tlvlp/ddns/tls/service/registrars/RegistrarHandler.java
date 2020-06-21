package com.tlvlp.ddns.tls.service.registrars;

import com.tlvlp.ddns.tls.service.records.DnsRecord;
import com.tlvlp.ddns.tls.service.records.DnsRecordDTO;

public interface RegistrarHandler {

    String getRecordContent(DnsRecord dnsRecord);
    String replaceRecordContent(DnsRecord dnsRecord, String newContent);

    default DnsRecordDTO convertRecordToDTO(DnsRecord dnsRecord, String data) {
        return new DnsRecordDTO()
                .setTtl(600)
                .setType(dnsRecord.getType())
                .setName(dnsRecord.getName())
                .setData(data);
    }
}
