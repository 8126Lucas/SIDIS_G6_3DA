package com.sidis.routeservice.Route.infrastructure.client.aircraft;

public record AircraftClientDTO (
    String registrationNumber,
    String modelName,
    Double totalFlightHours,
    Double meanRange,
    SeatingPackClientDTO seatingPackDTO
){}
