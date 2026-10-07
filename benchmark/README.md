# nQuack benchmark

This standalone Go 1.23+ tool applies a fixed request rate through a sending adapter, reports acceptance and latency, and can sample RabbitMQ queue depth while workers deliver through SMTP. It uses only the Go standard library.

## Before a run

Start Mailpit from this directory:

```sh
docker compose up -d
```

Configure the worker SMTP settings with host `localhost`, port `1025`, authentication and STARTTLS disabled, and a valid **Default sender address**, such as `nquack@example.com`. View captured mail at [http://localhost:8025](http://localhost:8025). Stop Mailpit with `docker compose down`.

Then start RabbitMQ, the publisher, and one or more workers. Each request uses a unique recipient: `benchmark1@example.com`, `benchmark2@example.com`, and so on. Mailpit accepts arbitrary recipients; use a test SMTP sink, not real recipients. Confirm the queue is empty and the publisher responds to a single request before starting the load run.

## Run

From this directory, start with a low rate:

```sh
env -u GOROOT go run . -rate 10 -duration 1m -drain 1m \
  -rabbit-url http://localhost:15672 -rabbit-password guest
```

Before sending load, the tool checks the publisher, RabbitMQ AMQP (`localhost:5672`), and SMTP sink (`localhost:1025`). If `-rabbit-url` is set, it also checks the management API and verifies that a worker is consuming the configured queue. Override the AMQP or SMTP address with `-rabbit-address` or `-smtp-address` if needed. The RabbitMQ Compose setup uses `guest`/`guest` for local management access. For a different password, use `PONGPIN_RABBITMQ_PASSWORD` or `-rabbit-password`.

The generated recipient addresses use the reserved `example.com` domain. Mailpit stores messages locally and accepts arbitrary recipients. The dashboard sender must be a valid email address.

Increase the rate in separate runs, for example 25, 50, 100, then 200 requests per second. Stop increasing when HTTP errors or capacity drops appear, p95/p99 latency rises sharply, or the ready-message count continues to grow after the load stops. Keep the machine, message size, SMTP sink, and worker count the same when comparing runs.

## Results

The tool prints scheduled requests, accepted sends, send failures, request errors, drops at the in-flight cap, accepted requests per second, and approximate p50/p95/p99 latency. RabbitMQ samples include publish/delivery rates plus ready and unacknowledged message counts. A JSON copy is written to `benchmark-results.json` by default.

The HTTP latency ends when the publisher responds; it does not measure SMTP completion. The RabbitMQ samples show whether consumers drain the queue, but do not prove how many SMTP messages arrived. Verify delivery totals in the test SMTP sink. `202 Accepted` is not a delivery receipt.

Useful options:

```text
-rate          target request arrivals per second (default 100)
-duration      time to send load (default 30s)
-drain         queue sampling after load stops (default 30s)
-max-inflight  cap concurrent HTTP requests (default 500)
-body-bytes    approximate HTML body size (default 1024)
-rabbit-address RabbitMQ AMQP host:port (default localhost:5672)
-smtp-address SMTP sink host:port (default localhost:1025)
-rabbit-url    optional RabbitMQ Management API URL
-rabbit-queue  queue to sample (default email.queue)
-out           JSON output path; pass -out "" to disable
```

## Extending

Sending and scheduling are separate: implement `adapter.Send` for another transport, or `loadMethod.Run` for another load pattern, then select it in `main`. RabbitMQ queue sampling remains provider-specific; add a sampler interface when another broker needs metrics.
