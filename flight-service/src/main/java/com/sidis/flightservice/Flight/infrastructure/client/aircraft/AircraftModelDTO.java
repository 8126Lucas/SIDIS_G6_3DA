package com.sidis.flightservice.Flight.infrastructure.client.aircraft;

public record AircraftModelDTO(
        String modelName,
        String manufacturer,
        AircraftModelSpecsDTO aircraftModelSpecsDTO
){}
