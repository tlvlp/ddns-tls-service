# DDNS and TLS service

## Features
- Monitors the host machine's external IP and compares/updates it at a given list of domains/records
- Creates TLS certificates for a given list of domains/records and persists them to a docker volume

## Supported registrars
Currently GoDaddy is the only supported registrar, but the list can easily be extended 
by adding new implementations of the RegistrarHandler interface.

## Dockerhub
Repository: [tlvlp/ddns-tls-service](https://cloud.docker.com/repository/docker/tlvlp/ddns-tls-service)

## Deployment

