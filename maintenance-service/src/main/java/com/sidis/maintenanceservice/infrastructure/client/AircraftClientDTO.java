package com.sidis.maintenanceservice.infrastructure.client;

public record AircraftClientDTO (
    String aircraftRegistration,
    String modelName,
    Double totalFlightHours
) {}
