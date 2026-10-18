package com.sidis.flightservice.Flight.infrastructure.client.airport;

import java.time.LocalDate;

public record CertificationDTO(
    Long certificationId,
    String name,
    String category,
    LocalDate startsAt,
    LocalDate expiresAt
){}
