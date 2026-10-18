package com.sidis.flightservice.Flight.infrastructure.client.airport;

public record RunwayDTO(
        Long runwayId,
        String runwayName,
        Double length,
        String orientation,
        String status
){}
