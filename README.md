# nQuack

> Queue-backed notification delivery with a dashboard for configuring publishers and workers.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)

## Project at a glance

| Measure | Current implementation |
| --- | --- |
| Deployable applications | 2: publisher and worker |
| Email submission endpoints | 1: `POST /api/email/send` |
| Implemented queue adapters | 1: RabbitMQ |
| RabbitMQ topology | 1 durable direct exchange, 1 durable email queue |
| Worker instances | Multiple can consume from the same queue |

## What it does

The publisher accepts an email request over HTTP and publishes it to RabbitMQ. One or more workers consume the request and send it through the configured SMTP server. The dashboard stores queue and SMTP settings and generates the worker's `application.yml`.

The API returns `202 Accepted` after publishing the request. This confirms queue acceptance; it does not confirm SMTP delivery.

## Quick start

Requirements: Java 21, Maven, and Docker. Configure an SMTP server you can access before sending real messages.

Start RabbitMQ:

```sh
docker compose -f publisher/src/main/java/org/example/QueueAdaptor/RMQ/docker-compose.yml up -d
```

Start the publisher:

```sh
mvn -pl publisher spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081), set the dashboard password on first launch, then configure RabbitMQ and SMTP. Download the generated worker YAML to a private location outside the repository:

```sh
mkdir -p "$HOME/.pongpin"
chmod 700 "$HOME/.pongpin"
```

Save the downloaded file as `$HOME/.pongpin/worker-application.yml`, then restrict access to it:

```sh
chmod 600 "$HOME/.pongpin/worker-application.yml"
```

Start a worker with that configuration:

```sh
SPRING_CONFIG_ADDITIONAL_LOCATION="file:$HOME/.pongpin/worker-application.yml" mvn -pl worker spring-boot:run
```

Submit an email:

```sh
curl -X POST http://localhost:8081/api/email/send \
  -H 'Content-Type: application/json' \
  -d '{"to":"person@example.com","subject":"Hello","htmlTemplate":"<p>Hello from Nquack.</p>"}'
```

Run another worker process to add another consumer of the RabbitMQ queue.

## Current scope

- RabbitMQ is the only implemented queue adapter. Kafka and SQS are shown as configuration options, but their worker adapters are not implemented.
- Queue priority routing is not implemented.
- SMTP delivery errors currently requeue the RabbitMQ message indefinitely; there is no dead-letter queue or retry backoff yet.
- `POST /api/email/send` does not require dashboard authentication. See the [API reference](./docs/api.md) for request and response details.

## Documentation

- [Architecture and message flow](./docs/architecture.md)
- [HTTP API](./docs/api.md)

## License

Licensed under the [Apache License 2.0](./LICENSE).

## Benchmarking

See the [benchmarking guide](./benchmark/README.md) to run load tests.
