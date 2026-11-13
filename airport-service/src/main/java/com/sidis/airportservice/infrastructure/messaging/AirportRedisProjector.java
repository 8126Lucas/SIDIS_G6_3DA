package com.sidis.airportservice.infrastructure.messaging;

import com.sidis.airportservice.Airport.domain.AirportReadModel;
import com.sidis.airportservice.Airport.infrastructure.AirportReadRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AirportRedisProjector {

    private final AirportReadRepository airportReadRepository;

    public AirportRedisProjector(
            AirportReadRepository airportReadRepository
    ) {
        this.airportReadRepository = airportReadRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.AIRPORT_SYNC_QUEUE)
    public void project(AirportReadModel airport) {
        airportReadRepository.save(airport);
    }
}