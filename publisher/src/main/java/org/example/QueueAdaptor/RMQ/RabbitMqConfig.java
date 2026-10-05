package org.example.QueueAdaptor.RMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
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
    Binding emailBinding(
            Queue emailQueue,
            DirectExchange applicationExchange,
            @Value("${app.rabbitmq.queues.email.routing-key}") String routingKey) {
        return BindingBuilder.bind(emailQueue).to(applicationExchange).with(routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
