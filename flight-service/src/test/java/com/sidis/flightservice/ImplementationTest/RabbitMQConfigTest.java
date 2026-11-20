package com.sidis.flightservice.ImplementationTest;

import com.sidis.flightservice.infrastructure.messaging.RabbitMQConfig;
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
    void shouldCreateFlightExchange() {
        TopicExchange exchange = rabbitMQConfig.flightExchange();
        assertNotNull(exchange);
        assertEquals(RabbitMQConfig.FLIGHT_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void shouldCreateFlightSyncQueue() {
        Queue queue = rabbitMQConfig.flightSyncQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.FLIGHT_SYNC_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isExclusive());
        assertFalse(queue.isAutoDelete());
        assertEquals(true, queue.getArguments().get("x-single-active-consumer"));
    }

    @Test
    void shouldCreateFlightSyncBinding() {
        Binding binding = rabbitMQConfig.flightSyncBinding();
        assertNotNull(binding);
        assertEquals(RabbitMQConfig.FLIGHT_SYNC_QUEUE, binding.getDestination());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
        assertEquals(RabbitMQConfig.FLIGHT_EXCHANGE, binding.getExchange());
        assertEquals(RabbitMQConfig.FLIGHT_UPDATED_KEY, binding.getRoutingKey());
    }

    @Test
    void shouldCreateFlightMessageConverter() {
        MessageConverter converter = rabbitMQConfig.flightMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }
}
