package com.sidis.aircraftservice.Aircraft.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class SeatingPack {

    private Integer seatingCapacity;
    private String seatingOrientation;

    protected SeatingPack() {}

    public SeatingPack(Integer seatingCapacity, String seatingOrientation) {
        this.seatingCapacity = seatingCapacity;
        this.seatingOrientation = seatingOrientation;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public String getSeatingOrientation() {
        return seatingOrientation;
    }
}