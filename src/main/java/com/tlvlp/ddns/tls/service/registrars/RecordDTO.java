package com.tlvlp.ddns.tls.service.registrars;

public class RecordDTO {

    private String type;
    private String name;
    private String data;
    private Integer ttl;

    public RecordDTO() {
    }

    @Override
    public String toString() {
        return "RecordDTO{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", data='" + data + '\'' +
                ", ttl=" + ttl +
                '}';
    }

    public String getType() {
        return type;
    }

    public RecordDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public RecordDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getData() {
        return data;
    }

    public RecordDTO setData(String data) {
        this.data = data;
        return this;
    }

    public Integer getTtl() {
        return ttl;
    }

    public RecordDTO setTtl(Integer ttl) {
        this.ttl = ttl;
        return this;
    }
}
