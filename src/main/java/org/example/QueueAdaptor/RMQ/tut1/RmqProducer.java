package org.example.QueueAdaptor.RMQ.tut1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RmqProducer {

    private static final Logger log = LoggerFactory.getLogger(RmqProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RmqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String message) {
        log.info("[PRODUCER] Sending message -> '{}' to exchange '{}' with routing key '{}'",
                message, RmqConfig.EXCHANGE_NAME, RmqConfig.ROUTING_KEY);

        // RabbitTemplate converts and publishes the message to the specified exchange & routing key
        rabbitTemplate.convertAndSend(RmqConfig.EXCHANGE_NAME, RmqConfig.ROUTING_KEY, message);
    }
}
