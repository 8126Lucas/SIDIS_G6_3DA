package com.sidis.flightservice.infrastructure.messaging;

import com.sidis.flightservice.Flight.application.FlightChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FlightEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public FlightEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(FlightChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FLIGHT_EXCHANGE,
                RabbitMQConfig.FLIGHT_UPDATED_KEY,
                event.flight()
        );
    }
}
