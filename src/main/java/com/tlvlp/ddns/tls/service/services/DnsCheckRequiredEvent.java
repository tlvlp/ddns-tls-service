package com.tlvlp.ddns.tls.service.services;

import org.springframework.context.ApplicationEvent;

public class DnsCheckRequiredEvent extends ApplicationEvent {

    private final Boolean updateForced;

    public DnsCheckRequiredEvent(Object source, Boolean updateForced) {
        super(source);
        this.updateForced = updateForced;
    }

    public Boolean isUpdateForced() {
        return updateForced;
    }


}
