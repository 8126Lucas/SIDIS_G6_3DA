package com.sidis.aircraftservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FlightClientDTO (
    @JsonProperty("flightId")
    Long id,
    @JsonProperty("aircraftRegistrationNumber")
    String aircraftRegistration,
    Long routeId,
    Double range,
    String departure,
    String arrival,
    Double flightHours
) {}
