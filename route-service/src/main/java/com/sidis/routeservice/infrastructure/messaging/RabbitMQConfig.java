package com.sidis.routeservice.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ROUTE_EXCHANGE = "route.exchange";
    public static final String ROUTE_SYNC_QUEUE = "route.sync.queue";
    public static final String ROUTE_UPDATED_KEY = "route.event.updated";

    @Bean
    public TopicExchange routeExchange() {
        return new TopicExchange(ROUTE_EXCHANGE, true, false);
    }

    @Bean
    public Queue routeSyncQueue() {
        return new Queue(
                ROUTE_SYNC_QUEUE,
                true,
                false,
                false,
                Map.of("x-single-active-consumer", true)
        );
    }

    @Bean
    public Binding routeSyncBinding() {
        return BindingBuilder
                .bind(routeSyncQueue())
                .to(routeExchange())
                .with(ROUTE_UPDATED_KEY);
    }

    @Bean
    public MessageConverter routeMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
