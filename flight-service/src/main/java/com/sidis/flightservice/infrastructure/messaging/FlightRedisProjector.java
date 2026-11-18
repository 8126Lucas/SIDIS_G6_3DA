package com.sidis.flightservice.infrastructure.messaging;

import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.infrastructure.FlightReadRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FlightRedisProjector {

    private final FlightReadRepository flightReadRepository;

    public FlightRedisProjector(FlightReadRepository flightReadRepository) {
        this.flightReadRepository = flightReadRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.FLIGHT_SYNC_QUEUE, concurrency = "1")
    public void project(FlightReadModel flight) {
        if (flight.version() == null) {
            throw new IllegalArgumentException("Flight event must include a version");
        }

        var existing = flightReadRepository.findById(flight.flightId());
        if (existing.isPresent()) {
            Long storedVersion = existing.get().version();
            if (storedVersion != null && storedVersion >= flight.version()) {
                return;
            }
        }

        flightReadRepository.save(flight);
    }
}
