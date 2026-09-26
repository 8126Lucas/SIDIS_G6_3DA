package com.sidis.aircraftservice.Aircraft.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class InstalledComponent {
    @Id
    private String serialNumber;

    private String partNumber;

    private String status;

    @ManyToOne
    @JoinColumn(name = "aircraft_registration_number")
    private Aircraft aircraft;

    protected InstalledComponent() {}

    public InstalledComponent(String serialNumber, String partNumber, String status, Aircraft aircraft) {
        this.serialNumber = serialNumber;
        this.partNumber = partNumber;
        this.status = status;
        this.aircraft = aircraft;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }
}