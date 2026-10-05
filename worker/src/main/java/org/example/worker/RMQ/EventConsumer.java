package org.example.worker.RMQ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    public void consume(String event) {
        logger.info("Received event: {}", event);
    }
}
