package com.sidis.maintenanceservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AircraftClientDTO (
    @JsonProperty("registrationNumber") String aircraftRegistration,
    String modelName,
    Double totalFlightHours
) {}
