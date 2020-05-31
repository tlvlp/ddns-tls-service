FROM openjdk:11-jre-slim

RUN mkdir /ddns_tls_service
WORKDIR /ddns_tls_service

COPY build/libs/*.jar ./app.jar

CMD ["java","-jar","app.jar"]
