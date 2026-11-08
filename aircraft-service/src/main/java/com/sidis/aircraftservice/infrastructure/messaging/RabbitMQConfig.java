package com.sidis.aircraftservice.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "aircraft.exchange";
    public static final String SYNC_QUEUE = "aircraft.sync.queue";
    public static final String ROUTING_KEY = "aircraft.event.#";

    // Exchange
    @Bean
    public TopicExchange aircraftExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // Queue
    @Bean
    public Queue aircraftSyncQueue() {
        return new Queue(SYNC_QUEUE, true);
    }

    @Bean
    public Binding bindingSyncQueue(Queue aircraftSyncQueue, TopicExchange aircraftExchange) {
        return BindingBuilder
                .bind(aircraftSyncQueue)
                .to(aircraftExchange)
                .with(ROUTING_KEY);
    }

    // JSON Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitMQ Template
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}