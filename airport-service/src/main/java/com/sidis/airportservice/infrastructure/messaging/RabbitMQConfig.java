package com.sidis.airportservice.infrastructure.messaging;

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

    public static final String AIRPORT_EXCHANGE = "airport.exchange";
    public static final String AIRPORT_SYNC_QUEUE = "airport.sync.queue";
    public static final String AIRPORT_UPDATED_KEY = "airport.event.updated";

    @Bean
    public TopicExchange airportExchange() {
        return new TopicExchange(AIRPORT_EXCHANGE, true, false);
    }

    @Bean
    public Queue airportSyncQueue() {
        return new Queue(
                AIRPORT_SYNC_QUEUE,
                true,
                false,
                false,
                Map.of("x-single-active-consumer", true)
        );
    }

    @Bean
    public Binding airportSyncBinding() {
        return BindingBuilder
                .bind(airportSyncQueue())
                .to(airportExchange())
                .with(AIRPORT_UPDATED_KEY);
    }

    @Bean
    public MessageConverter airportMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}