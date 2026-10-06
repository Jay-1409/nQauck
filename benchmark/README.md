# nQuack benchmark

This standalone Go 1.23+ tool applies a fixed request rate to the publisher API, reports HTTP acceptance and latency, and can sample RabbitMQ queue depth while workers deliver through SMTP. It uses only the Go standard library.

## Before a run

1. Start RabbitMQ, the publisher, and one or more workers.
2. Configure the worker to use a test SMTP sink or provider sandbox. The tool sends every request to the configured `-to` address; do not point it at real recipients.
3. Confirm the queue is empty and the publisher responds to a single request.

## Run

From this directory, start with a low rate:

```sh
env -u GOROOT go run . -rate 10 -duration 1m -drain 1m \
  -to benchmark@your-test-domain.example \
  -rabbit-url http://localhost:15672 -rabbit-password guest
```

The RabbitMQ Compose setup uses `guest`/`guest` for local management access. For a different password, use `PONGPIN_RABBITMQ_PASSWORD` or `-rabbit-password`.

Increase the rate in separate runs, for example 25, 50, 100, then 200 requests per second. Stop increasing when HTTP errors or capacity drops appear, p95/p99 latency rises sharply, or the ready-message count continues to grow after the load stops. Keep the machine, message size, SMTP sink, and worker count the same when comparing runs.

## Results

The tool prints scheduled requests, `202` responses, HTTP failures, request errors, drops at the in-flight cap, accepted requests per second, and approximate p50/p95/p99 HTTP latency. RabbitMQ samples include publish/delivery rates plus ready and unacknowledged message counts. A JSON copy is written to `benchmark-results.json` by default.

The HTTP latency ends when the publisher responds; it does not measure SMTP completion. The RabbitMQ samples show whether consumers drain the queue, but do not prove how many SMTP messages arrived. Verify delivery totals in the test SMTP sink. `202 Accepted` is not a delivery receipt.

Useful options:

```text
-rate          target request arrivals per second (default 100)
-duration      time to send load (default 30s)
-drain         queue sampling after load stops (default 30s)
-max-inflight  cap concurrent HTTP requests (default 500)
-body-bytes    approximate HTML body size (default 1024)
-rabbit-url    optional RabbitMQ Management API URL
-rabbit-queue  queue to sample (default email.queue)
-out           JSON output path; pass -out "" to disable
```
