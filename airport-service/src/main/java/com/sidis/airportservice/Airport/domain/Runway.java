package com.sidis.airportservice.Airport.domain;

import jakarta.persistence.*;

@Table(name = "company_airports_runways")
@Entity
public class Runway {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long runwayId;

    private String runwayName;

    private Double length;

    private String orientation;

    @Enumerated(EnumType.STRING)
    private RunwayStatus status;

    public Runway() {}

    public Runway(Long runwayId, String runwayName, Double length, String orientation, RunwayStatus status) {
        this.runwayId = runwayId;
        this.runwayName = runwayName;
        this.length = length;
        this.orientation = orientation;
        this.status = status;
    }

    public Long getRunwayId() {
        return runwayId;
    }

    public String getRunwayName() {
        return runwayName;
    }

    public Double getLength() {
        return length;
    }

    public String getOrientation() {
        return orientation;
    }

    public RunwayStatus getStatus() {
        return status;
    }
}