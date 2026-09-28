#!/usr/bin/env bash

# Determine directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

usage() {
    echo "Usage: $0 {start|stop|restart|status|logs}"
    echo "  start   - Build and start RabbitMQ container in background"
    echo "  stop    - Stop and remove RabbitMQ container"
    echo "  restart - Restart RabbitMQ container"
    echo "  status  - Show container status"
    echo "  logs    - Tail container logs"
    exit 1
}

case "$1" in
    start)
        echo "Starting RabbitMQ..."
        docker compose -f "$COMPOSE_FILE" up -d --build
        echo "RabbitMQ Management UI: http://localhost:15672 (User: guest / Pass: guest)"
        echo "RabbitMQ AMQP Port: 5672"
        ;;
    stop)
        echo "Stopping RabbitMQ..."
        docker compose -f "$COMPOSE_FILE" down
        ;;
    restart)
        echo "Restarting RabbitMQ..."
        docker compose -f "$COMPOSE_FILE" down
        docker compose -f "$COMPOSE_FILE" up -d --build
        ;;
    status)
        docker compose -f "$COMPOSE_FILE" ps
        ;;
    logs)
        docker compose -f "$COMPOSE_FILE" logs -f
        ;;
    *)
        usage
        ;;
esac
