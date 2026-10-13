# nQuack

> A highly configurable high-throughput notification delivery system. 

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)

## What it does

This is a high-throughput notification sending system. The server exposed APIs that can receive a request to send a notification, which can be an email, a message, a WhatsApp message or a Telegram message. This server will send it to the expected recipient 

## Features

- We support multiple message queues, such as RabbitMQ, Kafka, SQS (not currently implemeted). 
- You may setup priority delivery for messages, which allows important messages/emails to be delivered faster.
- Dashboard supporting GUI based statistics & Configuration.

## Screenshots
<img width="1664" height="1704" alt="image" src="https://github.com/user-attachments/assets/3a45e3c3-1ee0-452a-85f1-877b0e7ed8bb" />


## Documentation

- [Architecture and message flow](./docs/architecture.md)
- [API Reference](./docs/api.md)
- [Priority Scheduling](./docs/priority.md)
- [Quickstart guide](./docs/quickstart.md)
- [benchmarking guide](./benchmark/README.md)

## License

Licensed under the [Apache License 2.0](./LICENSE).

