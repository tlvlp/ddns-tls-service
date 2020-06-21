package com.tlvlp.ddns.tls.service.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.security.Security;

public class SecurityConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void configureSecurity() {
        Security.setProperty("crypto.policy", "unlimited");
        Security.addProvider(new BouncyCastleProvider());
    }

}
