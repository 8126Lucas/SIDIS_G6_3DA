package com.sidis.aircraftservice.AircraftCatalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AircraftSpecs {
    @Column(nullable = false)
    private Double fuelCapacityLiters;

    @Column(nullable = false)
    private Double maximumRangeKm;

    @Column(nullable = false)
    private Double cruisingSpeedKph;

    private Integer standardSeatingCapacity;

    public AircraftSpecs() {}

    public AircraftSpecs(Double fuelCapacity, Double maximumRange, Double cruisingSpeed, Integer standardSeatingCapacity) {
        this.fuelCapacityLiters = fuelCapacity;
        this.maximumRangeKm = maximumRange;
        this.cruisingSpeedKph = cruisingSpeed;
        this.standardSeatingCapacity = standardSeatingCapacity;
    }

    public Double getFuelCapacity() { return fuelCapacityLiters; }

    public Double getMaximumRange() {
        return maximumRangeKm;
    }

    public Double getCruisingSpeed() {
        return cruisingSpeedKph;
    }

    public Integer getStandardSeatingCapacity() {
        return standardSeatingCapacity;
    }

    public void changeStandardSeatingCapacity(Integer standardSeatingCapacity) {
        this.standardSeatingCapacity = standardSeatingCapacity;
    }

    public void changeMaximumRangeKm(Double maximumRangeKm) {
        this.maximumRangeKm = maximumRangeKm;
    }

    public void changeFuelCapacityLiters(Double fuelCapacityLiters) {
        this.fuelCapacityLiters = fuelCapacityLiters;
    }

    public void changeCruisingSpeedKph(Double cruisingSpeedKph) {
        this.cruisingSpeedKph = cruisingSpeedKph;
    }
}