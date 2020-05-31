package com.tlvlp.ddns.tls.service.services;

import com.tlvlp.ddns.tls.service.registrars.Record;
import com.tlvlp.ddns.tls.service.registrars.RegistrarHandler;
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
public class DNSUpdaterService {
    private static final Logger log = LoggerFactory.getLogger(DNSUpdaterService.class);

    private String previousIp;
    private final List<String> externalIpApiList;
    private final RecordService recordService;
    private final Boolean ddnsServiceActive;
    private final ApplicationEventPublisher publisher;

    public DNSUpdaterService(@Value("${ddns.service.active}") Boolean ddnsServiceActive,
                             @Value("${external.ip.apis.csv}") String externalIpApiCsv,
                             RecordService recordService,
                             ApplicationEventPublisher publisher) {
        this.recordService = recordService;
        this.externalIpApiList = Arrays.asList(externalIpApiCsv.split(","));
        this.ddnsServiceActive = ddnsServiceActive;
        this.publisher = publisher;
    }

    @Scheduled(cron = "${ip.check.schedule}")
    public void scheduledDnsCheckAndUpdate() {
        if(ddnsServiceActive) {
            publisher.publishEvent(new DnsCheckRequiredEvent(this, false));
        }
    }

    @EventListener
    public void runDnsCheckAndUpdate(DnsCheckRequiredEvent event) {
        try {
            log.debug("Running DDNS check.");
            String hostIp = getHostExternalIp();
            if(hostIp.equals(previousIp) && !event.isUpdateForced()) {
                log.debug("Host IP matches previously saved IP. No update required.");
                return;
            }
            previousIp = hostIp;
            recordService.getDdnsRecords().forEach(record -> checkAndUpdateRecord(record, hostIp));
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

    private void checkAndUpdateRecord(Record record, String hostIp) {
        try {
            RegistrarHandler registrarHandler = record.getRegistrar().getHandler();
            String ipAtRegistrar = registrarHandler.getRecordContent(record);
            if(hostIp.equals(ipAtRegistrar)) {
                log.debug("Host IP matches record IP at registrar. No update required.");
                return;
            }
            registrarHandler.replaceRecordContent(record, hostIp);
        } catch (Exception e) {
            log.error(String.format("Unable to complete scheduled DDNS check or update for record: %s%n", record), e);
        }
    }

}
