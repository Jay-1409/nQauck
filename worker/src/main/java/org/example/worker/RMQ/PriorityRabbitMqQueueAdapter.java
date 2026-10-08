package org.example.worker.RMQ;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.worker.QueueAdapter;
import org.example.worker.QueueProvider;
import org.example.worker.entities.EmailRequest;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.GetResponse;

@Component
@ConditionalOnProperty(prefix = "app.queue", name = "provider", havingValue = "RABBITMQ")
@ConditionalOnProperty(prefix = "app.rabbitmq.priority-scheduling", name = "enabled", havingValue = "true")
public class PriorityRabbitMqQueueAdapter implements QueueAdapter {

    private final RabbitTemplate rabbitTemplate;
    private final EventConsumer eventConsumer;
    private final String[] sequence;
    private volatile boolean running;
    private Thread worker;

    public PriorityRabbitMqQueueAdapter(RabbitTemplate rabbitTemplate, EventConsumer eventConsumer,
            @Value("${app.rabbitmq.priority-scheduling.sequence}") String[] sequence) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventConsumer = eventConsumer;
        this.sequence = sequence;
        if (sequence.length == 0) throw new IllegalArgumentException("Priority scheduling requires a non-empty queue sequence");
    }

    @Override
    public QueueProvider provider() {
        return QueueProvider.RABBITMQ;
    }

    @PostConstruct
    void start() {
        running = true;
        worker = Thread.ofVirtual().name("rabbitmq-priority-scheduler").start(this::run);
    }

    @PreDestroy
    void stop() throws InterruptedException {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker.join();
        }
    }

    private void run() {
        while (running) {
            boolean received = false;
            try {
                for (String queue : sequence) received |= receive(queue.trim());
                if (!received) Thread.sleep(1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private boolean receive(String queue) {
        // ponytail: Basic.Get polling preserves the exact sequence; switch to coordinated listeners if polling limits throughput.
        return rabbitTemplate.execute(channel -> {
            GetResponse response = channel.basicGet(queue, false);
            if (response == null) return false;
            long tag = response.getEnvelope().getDeliveryTag();
            try {
                Message message = new Message(response.getBody(),
                        new DefaultMessagePropertiesConverter().toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));
                MessageConverter converter = rabbitTemplate.getMessageConverter();
                eventConsumer.consume((EmailRequest) converter.fromMessage(message));
                channel.basicAck(tag, false);
            } catch (Exception exception) {
                channel.basicNack(tag, false, true);
            }
            return true;
        });
    }
}
