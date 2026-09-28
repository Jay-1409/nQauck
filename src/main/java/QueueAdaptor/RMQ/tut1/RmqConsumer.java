package QueueAdaptor.RMQ.tut1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RmqConsumer {

    private static final Logger log = LoggerFactory.getLogger(RmqConsumer.class);

    // @RabbitListener asynchronously listens to messages delivered to 'pongpin-queue'
    @RabbitListener(queues = RmqConfig.QUEUE_NAME)
    public void consumeMessage(String message) {
        log.info("[CONSUMER] Received message from queue '{}' -> '{}'", RmqConfig.QUEUE_NAME, message);
    }
}
