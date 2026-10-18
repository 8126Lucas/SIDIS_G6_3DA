package com.sidis.routeservice.Route.infrastructure.client.aircraft;

public record AircraftClientDTO (
    String registrationNumber,
    AircraftModelDTO modelDTO,
    Double totalFlightHours,
    Double meanRange,
    SeatingPackClientDTO seatingPackDTO
){}
