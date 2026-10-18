package com.sidis.routeservice.Route.infrastructure.client.aircraft;

public record AircraftModelSpecsDTO(
        Double fuelCapacityLiters,
        Double maximumRangeKm,
        Double cruisingSpeedKph,
        Integer standardSeatingCapacity
){}
