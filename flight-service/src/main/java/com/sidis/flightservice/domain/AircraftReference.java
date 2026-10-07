package com.sidis.flightservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AircraftReference {
    @Column(name = "registration_Number", nullable = false, length = 10)
    private String registrationNumber;

    protected AircraftReference() {}

    public AircraftReference(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftReference other)) return false;
        return registrationNumber.equals(other.registrationNumber);
    }

    @Override
    public int hashCode() {
        return registrationNumber.hashCode();
    }
}
