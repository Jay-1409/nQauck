package org.example.QueueAdaptor.RMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange applicationExchange(@Value("${app.rabbitmq.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue emailQueue(@Value("${app.rabbitmq.queues.email.name}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Queue highPriorityEmailQueue(@Value("${app.rabbitmq.queues.priority.high.name}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Queue mediumPriorityEmailQueue(@Value("${app.rabbitmq.queues.priority.medium.name}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Queue lowPriorityEmailQueue(@Value("${app.rabbitmq.queues.priority.low.name}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding emailBinding(
            @Qualifier("emailQueue") Queue emailQueue,
            DirectExchange applicationExchange,
            @Value("${app.rabbitmq.queues.email.routing-key}") String routingKey) {
        return BindingBuilder.bind(emailQueue).to(applicationExchange).with(routingKey);
    }

    @Bean
    Binding highPriorityBinding(@Qualifier("highPriorityEmailQueue") Queue queue, DirectExchange exchange,
            @Value("${app.rabbitmq.queues.priority.high.routing-key}") String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    Binding mediumPriorityBinding(@Qualifier("mediumPriorityEmailQueue") Queue queue, DirectExchange exchange,
            @Value("${app.rabbitmq.queues.priority.medium.routing-key}") String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    Binding lowPriorityBinding(@Qualifier("lowPriorityEmailQueue") Queue queue, DirectExchange exchange,
            @Value("${app.rabbitmq.queues.priority.low.routing-key}") String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
