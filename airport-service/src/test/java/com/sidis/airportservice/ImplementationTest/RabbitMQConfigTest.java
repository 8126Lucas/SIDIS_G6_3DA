package com.sidis.airportservice.ImplementationTest;

import com.sidis.airportservice.infrastructure.messaging.RabbitMQConfig;

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
    void shouldCreateAirportExchange() {
        TopicExchange exchange = rabbitMQConfig.airportExchange();
        assertNotNull(exchange);
        assertEquals("airport.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void shouldCreateAirportSyncQueue() {
        Queue queue = rabbitMQConfig.airportSyncQueue();
        assertNotNull(queue);
        assertEquals("airport.sync.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isExclusive());
        assertFalse(queue.isAutoDelete());
        assertEquals(true, queue.getArguments().get("x-single-active-consumer"));
    }

    @Test
    void shouldCreateAirportSyncBinding() {
        Binding binding = rabbitMQConfig.airportSyncBinding();
        assertNotNull(binding);
        assertEquals("airport.sync.queue", binding.getDestination());
        assertEquals("airport.exchange", binding.getExchange());
        assertEquals("airport.event.updated", binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    void shouldCreateJacksonMessageConverter() {
        MessageConverter converter = rabbitMQConfig.airportMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }
}
