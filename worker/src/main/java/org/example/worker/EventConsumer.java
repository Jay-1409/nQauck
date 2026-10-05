package org.example.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @RabbitListener(queues = "email.queue")
    public void consume(String event) {
        logger.info("Received event from email.queue: {}", event);
    }
}
