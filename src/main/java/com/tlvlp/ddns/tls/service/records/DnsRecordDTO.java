package com.tlvlp.ddns.tls.service.records;

public class DnsRecordDTO {

    private String type;
    private String name;
    private String data;
    private Integer ttl;

    public DnsRecordDTO() {
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

    public DnsRecordDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DnsRecordDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getData() {
        return data;
    }

    public DnsRecordDTO setData(String data) {
        this.data = data;
        return this;
    }

    public Integer getTtl() {
        return ttl;
    }

    public DnsRecordDTO setTtl(Integer ttl) {
        this.ttl = ttl;
        return this;
    }
}
