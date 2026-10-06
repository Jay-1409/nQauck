# Pongpin

> A small service for submitting and delivering HTML email.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)
![Version: 1.0-SNAPSHOT](https://img.shields.io/badge/version-1.0--SNAPSHOT-blue)
![Primary language: Java](https://img.shields.io/badge/primary%20language-Java-blue)

## What's in it for you

- Send HTML emails through one HTTP request.
- Configure SMTP from a browser dashboard.
- Preview locally captured messages with Mailpit.
- Add worker instances to increase delivery capacity.

## Features

- First-run dashboard account setup and login.
- Stepwise setup for queue and SMTP settings.
- Encrypted local dashboard configuration and BCrypt-hashed login passwords.
- Worker configuration YAML generation.
- Email submission API with asynchronous delivery.
- RabbitMQ is supported today. Kafka and SQS are listed in the dashboard as future options; their worker adapters are not implemented yet.

## Quick Start

Requirements: Java 21, Maven, and Docker.

Start RabbitMQ and Mailpit:

```sh
docker compose -f publisher/src/main/java/org/example/QueueAdaptor/RMQ/docker-compose.yml up -d
docker run -d --name pongpin-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit:latest
```

Start the publisher:

```sh
mvn -pl publisher spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081) and create a dashboard account. Create a private location for the generated file:

```sh
mkdir -p "$HOME/.pongpin"
chmod 700 "$HOME/.pongpin"
```

Configure SMTP with host `localhost`, port `1025`, authentication off, and STARTTLS off. Generate the worker YAML and save it outside the repository as:

```text
$HOME/.pongpin/worker-application.yml
```

Restrict access to the downloaded file:

```sh
chmod 600 "$HOME/.pongpin/worker-application.yml"
```

In another terminal, start the worker:

```sh
SPRING_CONFIG_ADDITIONAL_LOCATION="file:$HOME/.pongpin/worker-application.yml" mvn -pl worker spring-boot:run
```

Submit a test email:

```sh
curl -X POST http://localhost:8081/api/email/send \
  -H 'Content-Type: application/json' \
  -d '{"to":"test@example.com","subject":"Pongpin test","htmlTemplate":"<p>Hello from Pongpin.</p>"}'
```

The API returns `202 Accepted` when the publisher accepts the request. View the captured message at [http://localhost:8025](http://localhost:8025).

## Documentation

- [Architecture](./docs/architecture.md)
- [HTTP API](./docs/api.md)

## License

Licensed under the [Apache License 2.0](./LICENSE).
