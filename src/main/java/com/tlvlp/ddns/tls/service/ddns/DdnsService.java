package com.tlvlp.ddns.tls.service.ddns;

import com.tlvlp.ddns.tls.service.records.ConfigService;
import com.tlvlp.ddns.tls.service.records.DnsUpdaterService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DdnsService {
    private static final Logger log = LoggerFactory.getLogger(DdnsService.class);

    private String previousIp;
    private final List<String> externalIpApiList;
    private final ConfigService configService;
    private final Boolean isServiceActive;
    private final ApplicationEventPublisher publisher;
    private final DnsUpdaterService dnsUpdaterService;

    public DdnsService(@Value("${ddns.service.active}") Boolean isServiceActive,
                       @Value("${ip.apis.csv}") String externalIpApiCsv,
                       ConfigService configService,
                       ApplicationEventPublisher publisher, DnsUpdaterService dnsUpdaterService) {
        this.configService = configService;
        this.externalIpApiList = Arrays.asList(externalIpApiCsv.split(","));
        this.isServiceActive = isServiceActive;
        this.publisher = publisher;
        this.dnsUpdaterService = dnsUpdaterService;
    }

    @Scheduled(cron = "${ddns.check.schedule}")
    public void scheduledDnsCheckAndUpdate() {
        if(isServiceActive) {
            publisher.publishEvent(new DnsCheckRequiredEvent(this, false));
        }
    }

    @EventListener
    public void runDnsCheckAndUpdate(DnsCheckRequiredEvent event) {
        if(!isServiceActive) {
            return;
        }
        try {
            log.debug("Running DDNS check.");
            String hostIp = getHostExternalIp();
            if(hostIp.equals(previousIp) && !event.isUpdateForced()) {
                log.debug(String.format("Host IP matches previously saved IP(%s). No update required.", hostIp));
                return;
            }
            configService.getDdnsRecords().forEach(record -> dnsUpdaterService.checkAndUpdateRecord(record, hostIp));
            previousIp = hostIp;
        } catch (Exception e) {
            log.error("Unable to complete DDNS check: " + e.getMessage());
        }
    }

    private String getHostExternalIp() {
        String errorDetails = "";
        for (String api : externalIpApiList) {
            HttpResponse<String> response = Unirest
                    .get(api)
                    .asString();
            if(response.isSuccess()) {
                return response.getBody().strip();
            }
            errorDetails = String.format("Unable to determine host ip: %s - %s%nHeader:%n%s%nBody: %n%s%n",
                    response.getStatus(), response.getStatusText(),response.getHeaders(), response.getBody());
        }
        throw new RuntimeException(errorDetails);
    }



}
