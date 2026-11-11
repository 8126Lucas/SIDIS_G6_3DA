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
    public static final String AIRCRAFT_EXCHANGE_NAME = "aircraft.exchange";
    public static final String AIRCRAFT_SYNC_QUEUE = "aircraft.sync.queue";
    public static final String AIRCRAFT_ROUTING_KEY = "aircraft.event.#";

    public static final String MODEL_EXCHANGE_NAME = "aircraftmodel.exchange";
    public static final String MODEL_SYNC_QUEUE = "aircraftmodel.sync.queue";
    public static final String MODEL_ROUTING_KEY = "aircraftmodel.event.#";

    // Exchange
    @Bean
    public TopicExchange aircraftExchange() {
        return new TopicExchange(AIRCRAFT_EXCHANGE_NAME);
    }
    @Bean
    public TopicExchange aircraftModelExchange() { return new TopicExchange(MODEL_EXCHANGE_NAME); }

    // Queue
    @Bean
    public Queue aircraftSyncQueue() {
        return new Queue(AIRCRAFT_SYNC_QUEUE, true);
    }
    @Bean
    public Queue aircraftModelSyncQueue() { return new Queue(MODEL_SYNC_QUEUE, true); }

    @Bean
    public Binding aircraftBindingSyncQueue(Queue aircraftSyncQueue, TopicExchange aircraftExchange) {
        return BindingBuilder
                .bind(aircraftSyncQueue)
                .to(aircraftExchange)
                .with(AIRCRAFT_ROUTING_KEY);
    }
    @Bean
    public Binding aircraftModelBindingSyncQueue(Queue aircraftModelSyncQueue, TopicExchange aircraftModelExchange) {
        return BindingBuilder
                .bind(aircraftModelSyncQueue)
                .to(aircraftModelExchange)
                .with(MODEL_ROUTING_KEY);
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