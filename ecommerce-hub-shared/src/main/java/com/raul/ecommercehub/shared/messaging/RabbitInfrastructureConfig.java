package com.raul.ecommercehub.shared.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitInfrastructureConfig {

    public static final String SYNC_EXCHANGE = "sync-exchange";
    public static final String SYNC_QUEUE = RabbitMQNames.SYNC_QUEUE;
    public static final String SYNC_ROUTING_KEY = "sync";

    public static final String DLQ_EXCHANGE = "sync-dlq-exchange";
    public static final String DLQ_QUEUE = RabbitMQNames.SYNC_DLQ;
    public static final String DLQ_ROUTING_KEY = "sync-dlq";

    @Bean
    public DirectExchange syncExchange() {
        return new DirectExchange(SYNC_EXCHANGE);
    }

    @Bean
    public Queue syncQueue() {
        return QueueBuilder.durable(SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding syncBinding() {
        return BindingBuilder.bind(syncQueue()).to(syncExchange()).with(SYNC_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlqExchange()).with(DLQ_ROUTING_KEY);
    }
}