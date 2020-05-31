package com.tlvlp.ddns.tls.service.registrars;

public class Record {

    private Registrars registrar;
    private String apiKey;
    private String apiSecret;
    private String domain;
    private String type;
    private String name;

    public Record() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record)) return false;

        Record record = (Record) o;

        if (registrar != record.registrar) return false;
        if (!domain.equals(record.domain)) return false;
        if (!type.equals(record.type)) return false;
        return name.equals(record.name);
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

    public Record setRegistrar(Registrars registrar) {
        this.registrar = registrar;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Record setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public Record setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getType() {
        return type;
    }

    public Record setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Record setName(String name) {
        this.name = name;
        return this;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public Record setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }
}
