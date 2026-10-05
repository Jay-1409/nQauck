package org.example.worker;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QueueAdapterSelector {

    private final QueueAdapter activeAdapter;

    public QueueAdapterSelector(
            @Value("${app.queue.provider:RABBITMQ}") QueueProvider provider,
            List<QueueAdapter> adapters) {
        activeAdapter = adapters.stream()
                .filter(adapter -> adapter.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No queue adapter is available for " + provider));
    }

    public QueueAdapter activeAdapter() {
        return activeAdapter;
    }
}
