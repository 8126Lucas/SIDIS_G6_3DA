package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.infrastructure.messaging.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {
    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldCreateMaintenanceExchange() {
        TopicExchange exchange = config.maintenanceExchange();
        assertNotNull(exchange);
        assertEquals(RabbitMQConfig.MAINTENANCE_EXCHANGE_NAME, exchange.getName());
    }

    @Test
    void shouldCreateMaintenanceTemplateExchange() {
        TopicExchange exchange = config.maintenanceTemplateExchange();
        assertNotNull(exchange);
        assertEquals(RabbitMQConfig.TEMPLATE_EXCHANGE_NAME, exchange.getName());
    }

    @Test
    void shouldCreateDurableMaintenanceQueue() {
        Queue queue = config.maintenanceSyncQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.MAINTENANCE_SYNC_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void shouldCreateDurableMaintenanceTemplateQueue() {
        Queue queue = config.maintenanceTemplateSyncQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.TEMPLATE_SYNC_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void shouldCreateMaintenanceBinding() {
        Queue queue = config.maintenanceSyncQueue();
        TopicExchange exchange = config.maintenanceExchange();
        Binding binding = config.maintenanceBindingSyncQueue(queue, exchange);
        assertNotNull(binding);
        assertEquals(queue.getName(), binding.getDestination());
        assertEquals(exchange.getName(), binding.getExchange());
        assertEquals(RabbitMQConfig.MAINTENANCE_ROUTING_KEY, binding.getRoutingKey());
    }

    @Test
    void shouldCreateMaintenanceTemplateBinding() {
        Queue queue = config.maintenanceTemplateSyncQueue();
        TopicExchange exchange = config.maintenanceTemplateExchange();
        Binding binding = config.maintenanceTemplateBindingSyncQueue(queue, exchange);
        assertNotNull(binding);
        assertEquals(queue.getName(), binding.getDestination());
        assertEquals(exchange.getName(), binding.getExchange());
        assertEquals(RabbitMQConfig.TEMPLATE_ROUTING_KEY, binding.getRoutingKey());
    }

    @Test
    void shouldCreateJsonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    void shouldCreateAmqpTemplateWithJsonConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        AmqpTemplate template = config.amqpTemplate(connectionFactory);
        assertNotNull(template);
        assertInstanceOf(RabbitTemplate.class, template);
        RabbitTemplate rabbitTemplate = (RabbitTemplate) template;
        assertSame(connectionFactory, rabbitTemplate.getConnectionFactory());
        assertInstanceOf(Jackson2JsonMessageConverter.class, rabbitTemplate.getMessageConverter());
    }
}
