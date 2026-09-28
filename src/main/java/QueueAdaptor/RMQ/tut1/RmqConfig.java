package QueueAdaptor.RMQ.tut1;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RmqConfig {

    public static final String QUEUE_NAME = "pongpin-queue";
    public static final String EXCHANGE_NAME = "pongpin-exchange";
    public static final String ROUTING_KEY = "pongpin-routing-key";

    // 1. Declare the Queue (durable = true means queue survives broker restarts)
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true);
    }

    // 2. Declare a Direct Exchange
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // 3. Bind the Queue to the Exchange using a Routing Key
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
