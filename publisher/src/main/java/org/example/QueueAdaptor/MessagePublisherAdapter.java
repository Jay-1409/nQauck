package org.example.QueueAdaptor;

import org.example.configuration.QueueProvider;

public interface MessagePublisherAdapter {
    QueueProvider provider();

    void publish(String routingKey, Object message);
}
