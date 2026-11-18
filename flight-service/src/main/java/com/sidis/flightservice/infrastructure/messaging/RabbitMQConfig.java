package com.sidis.flightservice.infrastructure.messaging;

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

    public static final String FLIGHT_EXCHANGE = "flight.exchange";
    public static final String FLIGHT_SYNC_QUEUE = "flight.sync.queue";
    public static final String FLIGHT_UPDATED_KEY = "flight.event.updated";

    @Bean
    public TopicExchange flightExchange() {
        return new TopicExchange(FLIGHT_EXCHANGE, true, false);
    }

    @Bean
    public Queue flightSyncQueue() {
        return new Queue(
                FLIGHT_SYNC_QUEUE,
                true,
                false,
                false,
                Map.of("x-single-active-consumer", true)
        );
    }

    @Bean
    public Binding flightSyncBinding() {
        return BindingBuilder
                .bind(flightSyncQueue())
                .to(flightExchange())
                .with(FLIGHT_UPDATED_KEY);
    }

    @Bean
    public MessageConverter flightMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
