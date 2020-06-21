package com.tlvlp.ddns.tls.service.tls;

import org.springframework.context.ApplicationEvent;

public class TlsCertCheckRequiredEvent extends ApplicationEvent {

    public TlsCertCheckRequiredEvent(Object source) {
        super(source);
    }
}
