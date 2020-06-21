package com.tlvlp.ddns.tls.service.registrars;

import com.tlvlp.ddns.tls.service.records.DnsRecord;
import com.tlvlp.ddns.tls.service.records.DnsRecordDTO;
import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.util.List;

public class GoDaddyRegistrarHandler implements RegistrarHandler {

    @Override
    public String getRecordContent(DnsRecord dnsRecord) {
        //Implements https://developer.godaddy.com/doc/endpoint/domains#/v1/recordGet
        DnsRecordDTO dnsRecordDTO = Unirest.get("https://api.godaddy.com/v1/domains/{domain}/records/{type}/{name}")
                .header("accept", "application/json")
                .header("Authorization", String.format("sso-key %s:%s", dnsRecord.getApiKey(), dnsRecord.getApiSecret()))
                .routeParam("domain", dnsRecord.getDomain())
                .routeParam("type", dnsRecord.getType())
                .routeParam("name", dnsRecord.getName())
                .asObject(new GenericType<List<DnsRecordDTO>>() {})
                .ifFailure(response -> throwResponseDetailsOnFailure(response, dnsRecord))
                .getBody()
                .stream().findFirst()
                .orElse(null);
        if(dnsRecordDTO == null) {
            return null;
        }
        return dnsRecordDTO.getData();
    }

    @Override
    public String replaceRecordContent(DnsRecord dnsRecord, String newContent) {
        //Implements https://developer.godaddy.com/doc/endpoint/domains#/v1/recordReplaceTypeName
        return Unirest.put("https://api.godaddy.com/v1/domains/{domain}/records/{type}/{name}")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", String.format("sso-key %s:%s", dnsRecord.getApiKey(), dnsRecord.getApiSecret()))
                .routeParam("domain", dnsRecord.getDomain())
                .routeParam("type", dnsRecord.getType())
                .routeParam("name", dnsRecord.getName())
                .body(List.of(convertRecordToDTO(dnsRecord, newContent)))
                .asString()
                .ifFailure(response -> throwResponseDetailsOnFailure(response, dnsRecord))
                .getBody();

    }

    private void throwResponseDetailsOnFailure(HttpResponse<?> response, DnsRecord dnsRecord) {
        throw new RuntimeException(String.format("Unable to get record contents for: %nRecord: %s%nStatus: %s%nHeader: %n%s%nBody: %n%s%n",
                dnsRecord, response.getStatus(), response.getHeaders(), response.getBody()));
    }
}
