package com.tlvlp.ddns.tls.service.registrars;

import java.util.List;

public interface RegistrarHandler {

    String getRecordContent(Record record);
    String replaceRecordContent(Record record, String newContent);

    default RecordDTO convertRecordToDTO(Record record, String data) {
        return new RecordDTO()
                .setTtl(600)
                .setType(record.getType())
                .setName(record.getName())
                .setData(data);
    }
}
