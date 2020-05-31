package com.tlvlp.ddns.tls.service.registrars;

public enum Registrars {

    GODADDY(new GoDaddyRegistrarHandler());

    private final RegistrarHandler handler;

    Registrars(RegistrarHandler handler) {
        this.handler = handler;
    }

    public RegistrarHandler getHandler() {
        return handler;
    }

}
