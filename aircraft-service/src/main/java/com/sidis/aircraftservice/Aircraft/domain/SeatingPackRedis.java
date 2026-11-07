package com.sidis.aircraftservice.Aircraft.domain;

public class SeatingPackRedis {
    private Integer seatingCapacity;
    private String seatingOrientation;

    public SeatingPackRedis() {}

    public SeatingPackRedis(Integer seatingCapacity, String seatingOrientation) {
        this.seatingCapacity = seatingCapacity;
        this.seatingOrientation = seatingOrientation;
    }

    public Integer getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(Integer seatingCapacity) { this.seatingCapacity = seatingCapacity; }
    public String getSeatingOrientation() { return seatingOrientation; }
    public void setSeatingOrientation(String seatingOrientation) { this.seatingOrientation = seatingOrientation; }
}
