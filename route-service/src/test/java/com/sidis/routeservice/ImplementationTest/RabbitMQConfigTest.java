package com.sidis.routeservice.ImplementationTest;

import com.sidis.routeservice.infrastructure.messaging.RabbitMQConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {
    private RabbitMQConfig rabbitMQConfig;

    @BeforeEach
    void setUp() {
        rabbitMQConfig = new RabbitMQConfig();
    }

    @Test
    void shouldCreateRouteExchange() {
        TopicExchange exchange = rabbitMQConfig.routeExchange();
        assertNotNull(exchange);
        assertEquals("route.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void shouldCreateRouteSyncQueue() {
        Queue queue = rabbitMQConfig.routeSyncQueue();
        assertNotNull(queue);
        assertEquals("route.sync.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isExclusive());
        assertFalse(queue.isAutoDelete());
        assertEquals(true, queue.getArguments().get("x-single-active-consumer"));
    }

    @Test
    void shouldCreateRouteSyncBinding() {
        Binding binding = rabbitMQConfig.routeSyncBinding();
        assertNotNull(binding);
        assertEquals("route.sync.queue", binding.getDestination());
        assertEquals("route.exchange", binding.getExchange());
        assertEquals("route.event.updated", binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    void shouldCreateJacksonMessageConverter() {
        MessageConverter converter = rabbitMQConfig.routeMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }
}
