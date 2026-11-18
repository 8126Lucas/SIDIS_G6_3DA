package com.sidis.airportservice.infrastructure.messaging;

import com.sidis.airportservice.Airport.application.AirportChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AirportEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AirportEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AirportChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AIRPORT_EXCHANGE,
                RabbitMQConfig.AIRPORT_UPDATED_KEY,
                event.airport()
        );
    }
}