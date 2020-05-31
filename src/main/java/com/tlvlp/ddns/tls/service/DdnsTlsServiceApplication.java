package com.tlvlp.ddns.tls.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DdnsTlsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdnsTlsServiceApplication.class, args);
    }

}
