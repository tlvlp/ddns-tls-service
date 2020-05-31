package com.tlvlp.ddns.tls.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SecretsLoaderMock {

    @Autowired
    private ConfigurableEnvironment environment;

    public void addProperties(Map<String, Object> propertiesMap) {
        environment.getPropertySources().addFirst(new MapPropertySource("secrets", propertiesMap));
    }

}
