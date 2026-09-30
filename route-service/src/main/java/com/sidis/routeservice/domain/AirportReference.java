package com.sidis.routeservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AirportReference {
    @Column(name = "iata_code", nullable = false, length = 3)
    private String iataCode;

    protected AirportReference() {}

    public AirportReference(String iataCode) {
        if (iataCode == null || iataCode.length() != 3) {
            throw new IllegalArgumentException("Invalid IATA code");
        }

        this.iataCode = iataCode.toUpperCase();
    }

    public String getIataCode() {
        return iataCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportReference other)) return false;
        return iataCode.equals(other.iataCode);
    }

    @Override
    public int hashCode() {
        return iataCode.hashCode();
    }
}
