package com.tlvlp.ddns.tls.service.records;

import com.tlvlp.ddns.tls.service.registrars.Registrars;

import java.util.Arrays;
import java.util.List;

public class DnsRecordTLS extends DnsRecord {

    private String userEmail;
    private String userKeyPairRef;
    private String domainKeyPairRef;
    private String domainsToCoverCsv;

    public DnsRecordTLS() {
    }

    @Override
    public String toString() {
        return "Record{" +
                "registrar='" + registrar.name() + '\'' +
                ", key='" + apiKey + '\'' +
                ", domain='" + domain + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userKeyPairRef='" + userKeyPairRef + '\'' +
                ", domainKeyPairRef='" + domainKeyPairRef + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DnsRecordTLS)) return false;
        if (!super.equals(o)) return false;

        DnsRecordTLS that = (DnsRecordTLS) o;

        return domainsToCoverCsv.equals(that.domainsToCoverCsv);
    }

    @Override
    public int hashCode() {
        int result = registrar.hashCode();
        result = 31 * result + domainsToCoverCsv.hashCode();
        return result;
    }

    public List<String> getDomainsToCover() {
        return Arrays.asList(domainsToCoverCsv.split(","));
    }

    public Registrars getRegistrar() {
        return registrar;
    }

    public DnsRecordTLS setRegistrar(Registrars registrar) {
        this.registrar = registrar;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public DnsRecordTLS setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public DnsRecordTLS setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getType() {
        return type;
    }

    public DnsRecordTLS setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public DnsRecordTLS setName(String name) {
        this.name = name;
        return this;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public DnsRecordTLS setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public DnsRecordTLS setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserKeyPairRef() {
        return userKeyPairRef;
    }

    public DnsRecordTLS setUserKeyPairRef(String userKeyPairRef) {
        this.userKeyPairRef = userKeyPairRef;
        return this;
    }

    public String getDomainKeyPairRef() {
        return domainKeyPairRef;
    }

    public DnsRecordTLS setDomainKeyPairRef(String domainKeyPairRef) {
        this.domainKeyPairRef = domainKeyPairRef;
        return this;
    }

    public DnsRecordTLS setDomainsToCoverCsv(String domainsToCoverCsv) {
        this.domainsToCoverCsv = domainsToCoverCsv;
        return this;
    }
}
