package com.sidis.routeservice.Route.infrastructure.client.aircraft;

public record AircraftModelDTO(
        String modelName,
        String manufacturer,
        AircraftModelSpecsDTO aircraftModelSpecsDTO
){}
