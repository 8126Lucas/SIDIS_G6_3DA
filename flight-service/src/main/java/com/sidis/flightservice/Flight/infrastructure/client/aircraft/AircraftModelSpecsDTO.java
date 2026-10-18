package com.sidis.flightservice.Flight.infrastructure.client.aircraft;

public record AircraftModelSpecsDTO(
        Double fuelCapacityLiters,
        Double maximumRangeKm,
        Double cruisingSpeedKph,
        Integer standardSeatingCapacity
){}
