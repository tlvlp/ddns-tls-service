package com.tlvlp.ddns.tls.service.registrars;

import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.util.List;

public class GoDaddyRegistrarHandler implements RegistrarHandler {

    @Override
    public String getRecordContent(Record record) {
        //Implements https://developer.godaddy.com/doc/endpoint/domains#/v1/recordGet
        RecordDTO recordDTO = Unirest.get("https://api.godaddy.com/v1/domains/{domain}/records/{type}/{name}")
                .header("accept", "application/json")
                .header("Authorization", String.format("sso-key %s:%s", record.getApiKey(), record.getApiSecret()))
                .routeParam("domain", record.getDomain())
                .routeParam("type", record.getType())
                .routeParam("name", record.getName())
                .asObject(new GenericType<List<RecordDTO>>() {})
                .ifFailure(response -> throwResponseDetailsOnFailure(response, record))
                .getBody()
                .stream().findFirst()
                .orElse(null);
        if(recordDTO == null) {
            return null;
        }
        return recordDTO.getData();
    }

    @Override
    public String replaceRecordContent(Record record, String newContent) {
        //Implements https://developer.godaddy.com/doc/endpoint/domains#/v1/recordReplaceTypeName
        return Unirest.put("https://api.godaddy.com/v1/domains/{domain}/records/{type}/{name}")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", String.format("sso-key %s:%s", record.getApiKey(), record.getApiSecret()))
                .routeParam("domain", record.getDomain())
                .routeParam("type", record.getType())
                .routeParam("name", record.getName())
                .body(List.of(convertRecordToDTO(record, newContent)))
                .asString()
                .ifFailure(response -> throwResponseDetailsOnFailure(response, record))
                .getBody();

    }

    private void throwResponseDetailsOnFailure(HttpResponse<?> response, Record record) {
        throw new RuntimeException(String.format("Unable to get record contents for: %nRecord: %s%nStatus: %s%nHeader: %n%s%nBody: %n%s%n",
                record, response.getStatus(), response.getHeaders(), response.getBody()));
    }
}
