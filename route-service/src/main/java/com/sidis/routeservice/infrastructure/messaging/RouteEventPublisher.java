package com.sidis.routeservice.infrastructure.messaging;

import com.sidis.routeservice.Route.application.RouteChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RouteEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RouteEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(RouteChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ROUTE_EXCHANGE,
                RabbitMQConfig.ROUTE_UPDATED_KEY,
                event.route()
        );
    }
}
