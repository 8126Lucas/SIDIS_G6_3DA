package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.infrastructure.messaging.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {
    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldConfigureAircraftExchange() {
        TopicExchange exchange = config.aircraftExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.AIRCRAFT_EXCHANGE_NAME);
    }

    @Test
    void shouldConfigureAircraftModelExchange() {
        TopicExchange exchange = config.aircraftModelExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.MODEL_EXCHANGE_NAME);
    }

    @Test
    void shouldConfigureAircraftQueue() {
        Queue queue = config.aircraftSyncQueue();
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.AIRCRAFT_SYNC_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void shouldConfigureAircraftModelQueue() {
        Queue queue = config.aircraftModelSyncQueue();
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.MODEL_SYNC_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void shouldConfigureAircraftBinding() {
        Binding binding = config.aircraftBindingSyncQueue(
                config.aircraftSyncQueue(),
                config.aircraftExchange()
        );
        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.AIRCRAFT_SYNC_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.AIRCRAFT_EXCHANGE_NAME);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.AIRCRAFT_ROUTING_KEY);
    }

    @Test
    void shouldConfigureAircraftModelBinding() {
        Binding binding = config.aircraftModelBindingSyncQueue(
                config.aircraftModelSyncQueue(),
                config.aircraftModelExchange()
        );

        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.MODEL_SYNC_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.MODEL_EXCHANGE_NAME);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.MODEL_ROUTING_KEY);
    }

    @Test
    void shouldConfigureJsonMessageConverter() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}