package com.sidis.aircraftservice.Aircraft.domain;

import java.time.LocalDate;

public class AircraftCertificationRedis {
    private String name;
    private String category;
    private LocalDate startAt;
    private LocalDate expiresAt;

    public AircraftCertificationRedis() {}

    public AircraftCertificationRedis(String name, String category, LocalDate startAt, LocalDate expiresAt) {
        this.name = name;
        this.category = category;
        this.startAt = startAt;
        this.expiresAt = expiresAt;
    }

    public boolean isValid() {
        return !LocalDate.now().isAfter(expiresAt);
    }

    public String getName() { return this.name; }
    public String getCategory() { return this.category; }
    public LocalDate getStartAt() { return this.startAt; }
    public LocalDate getExpiresAt() { return this.expiresAt; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setStartAt(LocalDate startAt) { this.startAt = startAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}