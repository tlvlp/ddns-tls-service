# DDNS and TLS service

## Features

### DDNS
Dynamic DNS for home servers or any machine that doesn't have a fixed IP.
- File-based configuration of  DNS record details that are updated based on a configurable CRON schedule.
- Handles any number of DNS records under any number of different registrars / accounts.
- Monitors the host machine's external IP with a configurable CRON schedule.
- On external IP change or update in the DNS record configuration it validates and replaces the contents of each DNS record.

### TLS
TLS certificate generator and maintainer - IN PROGRESS
- Creates TLS certificates for a given list of domains/records and persists them to a docker volume

## Supported registrars
Currently GoDaddy is the only supported registrar, but the list can be extended 
by adding new implementations of the RegistrarHandler interface.

## Dockerhub
Repository: [tlvlp/ddns-tls-service](https://cloud.docker.com/repository/docker/tlvlp/ddns-tls-service)

## Configuration and Deployment

1. Clone the git repository to the host machine.
2. Make sure that Docker and Docker Swarm are installed and that you have root privileges.
3. Generate production API tokens to the registrar accounts that you are planning to access.
4. Update [the DNS records](deployment/ddns_records/ddns_records.json) that you wish to be updated with the host machine's IP.
5. Save all your API secrets as docker secrets. They will be paired by the API key in each DNS record: 
```shell script
echo MY_API_SECRET | docker secret create MY_API_KEY -
```
6. Update the [docker compose file](deployment/docker-compose.yml) with all your newly created docker secrets
7. Copy the ddns_records folder under the project's folder in /opt. You can use the [copy script](deployment/copy_configs_to_opt.sh) for the same.
8. Deploy the [docker compose file](deployment/docker-compose.yml).  You can use the [deploy script](deployment/deploy_docker_stack.sh) for the same.
