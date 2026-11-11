package com.sidis.airportservice.Airport.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDate;
import java.util.List;

@RedisHash("Airport")
public record AirportReadModel(
        @Id String iataCode,
        String airportType,
        String name,
        @Indexed AirportStatus status,
        Location location,
        FacilitiesData facilities,
        Double routeDistance,
        List<RunwayData> runways,
        List<CertificationData> certifications,
        List<ContactData> contacts,
        List<String> photos,
        String operationalHours
) {
    public record Location(
            @Indexed String city,
            @Indexed String country,
            String timezone,
            String region,
            Double latitude,
            Double longitude
    ) {}

    public record FacilitiesData(
            Integer terminalCount,
            Integer gateCount,
            List<String> services
    ) {}

    public record RunwayData(
            Long runwayId,
            String runwayName,
            Double length,
            String orientation,
            RunwayStatus status
    ) {}

    public record CertificationData(
            Long certificationId,
            String name,
            String category,
            LocalDate startsAt,
            LocalDate expiresAt
    ) {}

    public record ContactData(
            ContactType type,
            String value
    ) {}
}