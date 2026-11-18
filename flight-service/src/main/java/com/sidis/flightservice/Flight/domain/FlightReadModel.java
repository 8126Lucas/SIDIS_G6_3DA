package com.sidis.flightservice.Flight.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@RedisHash("Flight")
public record FlightReadModel(
        @Id Long flightId,
        RouteData route,
        @Indexed String aircraftRegistrationNumber,
        FlightStatus status,
        LocalDateTime scheduledDeparture,
        LocalDateTime scheduledArrival,
        LocalDateTime timeOfDeparture,
        LocalDateTime timeOfArrival,
        Long version
) {
    public record RouteData(
            Long routeId,
            String routeName,
            String originIata,
            String destinationIata
    ) {}
}
