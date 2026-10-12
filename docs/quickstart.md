# Quickstart

Requirements: Java 21, Maven, and Docker. Configure an SMTP server you can access before sending real messages. Run the commands below from the repository root.

Start RabbitMQ:

```sh
docker compose -f publisher/src/main/java/org/example/QueueAdaptor/RMQ/docker-compose.yml up -d
```

Start the publisher:

```sh
mvn -pl publisher spring-boot:run
```

Open [http://localhost:17431](http://localhost:17431), set the dashboard password on first launch, then configure RabbitMQ and SMTP. Download the generated worker YAML to a private location outside the repository:

The dashboard also saves a publisher-safe runtime file at `$HOME/.pongpin/publisher-application.yml`. After configuring the dashboard, you can use it on the next publisher start:

```sh
SPRING_CONFIG_ADDITIONAL_LOCATION="file:$HOME/.pongpin/publisher-application.yml" mvn -pl publisher spring-boot:run
```

This file contains queue provider and broker settings only; SMTP credentials remain in the encrypted dashboard store and the downloaded worker YAML.

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
curl -X POST http://localhost:17431/api/email/send \
  -H 'Content-Type: application/json' \
  -d '{"to":"person@example.com","subject":"Hello","htmlTemplate":"<p>Hello from Nquack.</p>"}'
```

Run another worker process to add another consumer of the RabbitMQ queue.
