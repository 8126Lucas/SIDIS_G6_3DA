package com.sidis.routeservice.Route.infrastructure.client.airport;


public record AirportLocationDTO(
        String city,
        String country,
        String timezone,
        String region,
        Double latitude,
        Double longitude
){}