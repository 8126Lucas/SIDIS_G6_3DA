package com.sidis.aircraftservice.infrastructure.client;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightClientDTO(
        @JsonAlias("flightId") Long id,
        @JsonAlias("aircraftRegistrationNumber") String aircraftRegistration,
        Long routeId,
        Double range,
        @JsonAlias("scheduledDeparture") String departure,
        @JsonAlias("scheduledArrival") String arrival,
        Double flightHours
) {}
