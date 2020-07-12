# DDNS and TLS service

## Features

### DDNS
Dynamic DNS for home servers or any machine that doesn't have a fixed IP.
- Runs periodically at a given schedule
- File-based configuration of  DNS dnsRecord details that are updated based on a configurable CRON schedule.
- Handles any number of DNS dnsRecords under any number of different registrars / accounts.
- Monitors the host machine's external IP with a configurable CRON schedule.
- On external IP change or update in the DNS dnsRecord configuration it validates and replaces the contents of each DNS dnsRecord.

### TLS
TLS certificate generator and maintainer
- Runs periodically at a given schedule
- Retrieves TLS certificates for a given list of dnsRecords/domains 
- Generates a csr, cert, full chain cert and pkcs12 keystore with the full chain cert 
- Persists them to a docker volume

## Supported registrars
Currently GoDaddy is the only supported registrar, but the list can be extended 
by adding new implementations of the RegistrarHandler interface.

## Docker hub
Repository: [tlvlp/ddns-tls-service](https://hub.docker.com/repository/docker/tlvlp/ddns-tls-service)

## Configuration and Deployment
### Steps
1. Clone the git repository to the host machine.
2. Make sure Docker and Docker Swarm are installed and that you have root privileges.
3. Generate production API tokens to the registrar accounts that you are planning to access.
4. Update [the DDNS records](deployment/config/dns_records_ddns.json) (see below for details)
5. Update [the TLS DNS records](deployment/config/dns_records_tls.json) (see below for details)
6. Save all your API secrets as docker secrets. They will be paired by the API key in each DNS dnsRecord: 
```shell script
echo MY_API_SECRET | docker secret create MY_API_KEY -
```
7. Update the [docker compose file](deployment/docker-compose.yml) with all your newly created docker secrets
8. Copy the ddns_records folder under the project's folder in /opt. You can use the [copy script](deployment/copy_configs_to_opt.sh) for the same.
9. Deploy the [docker compose file](deployment/docker-compose.yml).  You can use the [deploy script](deployment/deploy_docker_stack.sh) for the same.

### Config files

[DDNS records](deployment/config/dns_records_ddns.json)
A list of DNS records that you wish to be updated with the host machine's IP.

|Key | Note |
|---|---|
|"registrar" | Registrar identifier |
|"apiKey" | "Production API key obtained from the registrar |
|"domain" |  The domain to be updated with the new IP |
|"type" |  Record type, like TXT or @ |
|"name" |  The domain record to be updated with the new IP |



[TLS DNS records](deployment/config/dns_records_tls.json)
A list of records that you wish to obtain certificates for.

|Key | Note |
|---|---|
|"registrar" | Registrar identifier |
|"apiKey" | "Production API key obtained from the registrar |
|"domain" |  The domain to be updated with the CA challenge text |
|"userKeyPairRef" | Reference to the user keypair. Can be a docker secret, environment variable or absolute file path. Will be generated if not found!|
|"domainKeyPairRef" | Same as the user KP but for the given domain |
|"userEmail" | Owner email address |
|"domainsToCoverCsv" | Single domain ora a csv list for the subdomains to be certified |
|"keyStorePass" | Password to be used to secure the p12 keystore |

