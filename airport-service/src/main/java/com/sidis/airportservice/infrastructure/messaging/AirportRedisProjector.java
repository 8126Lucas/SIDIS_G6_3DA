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

    @RabbitListener(
            queues = RabbitMQConfig.AIRPORT_SYNC_QUEUE,
            concurrency = "1"
    )
    public void project(AirportReadModel airport) {
        if (airport.version() == null) {
            throw new IllegalArgumentException(
                    "Airport event must include a version"
            );
        }

        var existing = airportReadRepository.findById(airport.iataCode());

        if (existing.isPresent()) {
            Long storedVersion = existing.get().version();

            if (storedVersion != null
                    && storedVersion >= airport.version()) {
                return;
            }
        }

        airportReadRepository.save(airport);
    }
}