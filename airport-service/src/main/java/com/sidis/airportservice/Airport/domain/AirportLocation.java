package com.sidis.airportservice.Airport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AirportLocation {
    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    public AirportLocation() {}

    public AirportLocation(String city, Double longitude, Double latitude, String region, String timezone, String country) {
        this.city = city;
        this.longitude = longitude;
        this.latitude = latitude;
        this.region = region;
        this.timezone = timezone;
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getRegion() {
        return region;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
