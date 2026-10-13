package org.example.worker.RMQ;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PriorityRabbitMqQueueAdapterTest {

    @Test
    void rejectsEmptySequence() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        EventConsumer consumer = mock(EventConsumer.class);

        assertThrows(IllegalArgumentException.class, () -> new PriorityRabbitMqQueueAdapter(
                template, consumer, new String[]{}, "high", "medium", "low", "legacy"));
    }

    @Test
    void rejectsInvalidPriorityLevels() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        EventConsumer consumer = mock(EventConsumer.class);

        assertThrows(IllegalArgumentException.class, () -> new PriorityRabbitMqQueueAdapter(
                template, consumer, new String[]{"0", "3"}, "high", "medium", "low", "legacy"));
    }
}
