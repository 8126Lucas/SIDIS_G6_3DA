package com.sidis.routeservice.Route.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDate;

@RedisHash("Route")
public record RouteReadModel(
        @Id Long routeId,
        String routeName,
        @Indexed RouteStatus status,
        RouteType type,
        AirportData originAirport,
        AirportData destinationAirport,
        HistoryData routeHistory,
        Double estimatedFlightTimeHours,
        Double distanceKm,
        RequirementsData routeRequirements,
        Long version
) {
    public record AirportData(
            @Indexed String iataCode,
            Location location
    ) {}

    public record Location(
            String city,
            String country,
            String timezone,
            String region,
            Double latitude,
            Double longitude
    ) {}

    public record HistoryData(
            LocalDate routeBegin,
            LocalDate routeFinish,
            Integer routeUsage
    ) {}

    public record RequirementsData(
            Double requiredRange,
            Integer requiredCapacity
    ) {}
}
