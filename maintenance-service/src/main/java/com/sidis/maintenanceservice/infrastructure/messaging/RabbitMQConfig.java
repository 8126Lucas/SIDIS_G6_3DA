package com.sidis.maintenanceservice.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String MAINTENANCE_EXCHANGE_NAME = "maintenance.exchange";
    public static final String MAINTENANCE_SYNC_QUEUE = "maintenance.sync.queue";
    public static final String MAINTENANCE_ROUTING_KEY = "maintenance.event.#";

    public static final String TEMPLATE_EXCHANGE_NAME = "maintenancetemplate.exchange";
    public static final String TEMPLATE_SYNC_QUEUE = "maintenancetemplate.sync.queue";
    public static final String TEMPLATE_ROUTING_KEY = "maintenancetemplate.event.#";

    // Exchange
    @Bean
    public TopicExchange maintenanceExchange() {
        return new TopicExchange(MAINTENANCE_EXCHANGE_NAME);
    }
    @Bean
    public TopicExchange maintenanceTemplateExchange() { return new TopicExchange(TEMPLATE_EXCHANGE_NAME); }

    // Queue
    @Bean
    public Queue maintenanceSyncQueue() {
        return new Queue(MAINTENANCE_SYNC_QUEUE, true);
    }
    @Bean
    public Queue maintenanceTemplateSyncQueue() { return new Queue(TEMPLATE_SYNC_QUEUE, true); }

    @Bean
    public Binding maintenanceBindingSyncQueue(Queue maintenanceSyncQueue, TopicExchange maintenanceExchange) {
        return BindingBuilder
                .bind(maintenanceSyncQueue)
                .to(maintenanceExchange)
                .with(MAINTENANCE_ROUTING_KEY);
    }
    @Bean
    public Binding maintenanceTemplateBindingSyncQueue(Queue maintenanceTemplateSyncQueue, TopicExchange maintenanceTemplateExchange) {
        return BindingBuilder
                .bind(maintenanceTemplateSyncQueue)
                .to(maintenanceTemplateExchange)
                .with(TEMPLATE_ROUTING_KEY);
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