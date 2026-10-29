package com.sidis.maintenanceservice.infrastructure.client;

public record AircraftClientDTO (
    String registrationNumber,
    String modelName,
    Double totalFlightHours
) {}
