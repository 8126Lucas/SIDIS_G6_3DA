package com.sidis.flightservice.Flight.infrastructure.client.airport;

public record AirportLocationDTO(
        String city,
        String country,
        String timezone,
        String region,
        Double latitude,
        Double longitude
){}