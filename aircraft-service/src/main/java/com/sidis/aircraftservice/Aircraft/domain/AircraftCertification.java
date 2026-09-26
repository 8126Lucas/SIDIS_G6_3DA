package com.sidis.aircraftservice.Aircraft.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
public class AircraftCertification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aircraftCertificationId;

    @ManyToOne
    @JoinColumn(name = "aircraft_registration_number")
    private Aircraft aircraft;

    private String name;
    private String category;

    private LocalDate startAt;
    private LocalDate expiresAt;

    protected AircraftCertification() {}

    public AircraftCertification(
            String name,
            String category,
            LocalDate startAt,
            LocalDate expiresAt
    ) {
        this.name = name;
        this.category = category;
        this.startAt = startAt;
        this.expiresAt = expiresAt;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    public boolean isValid() {
        return !LocalDate.now().isAfter(expiresAt);
    }
}