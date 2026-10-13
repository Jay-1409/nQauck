package org.example.worker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueueAdapterSelectorTest {

    static class DummyAdapter implements QueueAdapter {
        private final QueueProvider provider;

        DummyAdapter(QueueProvider provider) {
            this.provider = provider;
        }

        @Override
        public QueueProvider provider() {
            return provider;
        }
    }

    @Test
    void selectsActiveAdapterMatchingProvider() {
        QueueAdapter rmq = new DummyAdapter(QueueProvider.RABBITMQ);
        QueueAdapter kafka = new DummyAdapter(QueueProvider.KAFKA);
        QueueAdapterSelector selector = new QueueAdapterSelector(QueueProvider.KAFKA, List.of(rmq, kafka));

        assertEquals(kafka, selector.activeAdapter());
    }

    @Test
    void throwsWhenNoAdapterMatchesProvider() {
        QueueAdapter rmq = new DummyAdapter(QueueProvider.RABBITMQ);
        assertThrows(IllegalStateException.class, () -> new QueueAdapterSelector(QueueProvider.KAFKA, List.of(rmq)));
    }
}
