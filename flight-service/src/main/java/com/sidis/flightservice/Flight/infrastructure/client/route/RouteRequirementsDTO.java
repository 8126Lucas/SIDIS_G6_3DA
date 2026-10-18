package com.sidis.flightservice.Flight.infrastructure.client.route;

public record RouteRequirementsDTO(
        Double requiredRange,
        Integer requiredCapacity
){}
