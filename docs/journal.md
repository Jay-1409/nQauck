# This is a journal of this project 

## 27-SEP-2026

### 15:04
Today i watched this youtube video on [razorpays notification system ](https://www.youtube.com/watch?v=DQwlmTvs6xA). this
inspired me to make a tool that can help people seamlessly add a notification system into their projects. this system should be 
scale-able & efficient& should support all types of nitofications (Emails, Text messages, Whatsapp Messages, Telegrams mesages and many more...)


There are a couple of decisions that i am not able to make for now like deciding on the queuing service that i should use? do i use AWS SQS / RMQ/ KAFKA .. ? one idea here is to use adaptors to allow users to use 
either of them as they wish . 
- Local development: BullMQ + Redis, with Docker Compose.
- Production on AWS: SQS + dead-letter queues.
- Self-hosted deployments: RabbitMQ or BullMQ + Redis.

For example:
```
QUEUE_DRIVER=sqs
AWS_REGION=ap-south-1
SQS_QUEUE_URL=...

Or:

QUEUE_DRIVER=bullmq
REDIS_URL=redis://localhost:6379
```

i think ill start with what seems the most easiest to me. 


## 5-OCT-2026

### 20:49
Today i have done some good progress on this, i have managed to get the dashboard working and the idea is that after the user 
chooses the configurations that they need, like choosing RMQ..., a YAML file is being generated which will be then required by each 
of the workers in order to know information of the queue. All the workers just need to be given this YAML and they should know exactly what they have to do.

we can use mailpit for simulating an smtp server. -> this was cool


## 7-OCT-2026

### 16:01

## Dashboard Statistics

| Area               | Possible Statistics                                                                      | Data Needed                                                                                                            |
| ------------------ | ---------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Queue**          | Messages waiting, unacknowledged messages, publish rate, delivery rate, active consumers | RabbitMQ Management API. These are also the metrics already sampled by the benchmark.                                  |
| **Publisher**      | Requests accepted, failed requests, acceptance rate, response latency (p50/p95/p99)      | Add counters and latency tracking to the publisher. A `202 Accepted` response only means the message was queued.       |
| **Workers**        | Active consumers, messages processed per second, processing latency, worker health       | RabbitMQ can report consumer counts; throughput and health require worker instrumentation.                             |
| **Email Delivery** | Emails sent, send failures, retries, SMTP response time, most recent error               | Add delivery counters and status reporting to the worker.                                                              |
| **System Health**  | Publisher, worker, and broker availability; uptime; CPU and memory usage                 | Health checks are needed for availability and uptime. CPU and memory metrics require runtime or host-level monitoring. |



## 9-OCT-2026

### 15:08

Today I am going to try and learn about how to actually use **KAFKA** from the documentation. 

 