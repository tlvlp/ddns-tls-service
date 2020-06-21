package com.tlvlp.ddns.tls.service.records;

import com.tlvlp.ddns.tls.service.registrars.Registrars;

public class DnsRecord {

    protected Registrars registrar;
    protected String apiKey;
    protected String apiSecret;
    protected String domain;
    protected String type;
    protected String name;

    public DnsRecord() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DnsRecord)) return false;

        DnsRecord dnsRecord = (DnsRecord) o;

        if (registrar != dnsRecord.registrar) return false;
        if (!domain.equals(dnsRecord.domain)) return false;
        if (!type.equals(dnsRecord.type)) return false;
        return name.equals(dnsRecord.name);
    }

    @Override
    public int hashCode() {
        int result = registrar.hashCode();
        result = 31 * result + domain.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Record{" +
                "registrar='" + registrar.name() + '\'' +
                ", key='" + apiKey + '\'' +
                ", domain='" + domain + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public Registrars getRegistrar() {
        return registrar;
    }

    public DnsRecord setRegistrar(Registrars registrar) {
        this.registrar = registrar;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public DnsRecord setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public DnsRecord setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getType() {
        return type;
    }

    public DnsRecord setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DnsRecord setName(String name) {
        this.name = name;
        return this;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public DnsRecord setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }
}
