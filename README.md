# nQuack

> A highly configurable high-throughput notification delivery system. 

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)

## What it does

This is a high-throughput notification sending system. The server exposed APIs that can receive a request to send a notification, which can be an email, a message, a WhatsApp message or a Telegram message. This server will send it to the expected recipient 

## Quick start

Follow the [Quickstart guide](./docs/quickstart.md) to start RabbitMQ, the publisher, and a worker, then submit a test email.

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
