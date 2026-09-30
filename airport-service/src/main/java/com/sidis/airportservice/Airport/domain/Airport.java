package com.sidis.airportservice.Airport.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company_registered_airports")
public class Airport {
    @Id
    private String iataCode;

    private String airportType;

    private String name;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private AirportStatus status;

    @Embedded
    private AirportLocation airportLocation;

    @Embedded
    private Facilities facilities;

    private Double routeDistance;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "airport_iata_code")
    private List<Runway> airportRunways = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "airport_iata_code")
    private List<Certification> certifications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "airport_contacts",
            joinColumns = @JoinColumn(name = "airport_iata_code")
    )
    private List<Contact> contacts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "airport_images",
            joinColumns = @JoinColumn(name = "airport_iata_code")
    )
    @Column(name = "image_url")
    private List<String> airportPhotos = new ArrayList<>();

    public Airport() {
    }

    public Airport(String iataCode, String airportType, String name, AirportStatus status,
                   AirportLocation airportLocation, Facilities facilities, Double routeDistance,
                   List<Runway> airportRunways, List<Certification> certifications,
                   List<Contact> contacts, List<String> airportPhotos) {
        this.iataCode = iataCode;
        this.airportType = airportType;
        this.name = name;
        this.status = status;
        this.airportLocation = airportLocation;
        this.facilities = facilities;
        this.routeDistance = routeDistance;
        this.airportRunways = airportRunways == null ? new ArrayList<>() : new ArrayList<>(airportRunways);
        this.certifications = certifications == null ? new ArrayList<>() : new ArrayList<>(certifications);
        this.contacts = contacts == null ? new ArrayList<>() : new ArrayList<>(contacts);
        this.airportPhotos = airportPhotos == null ? new ArrayList<>() : new ArrayList<>(airportPhotos);
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getAirportType() {
        return airportType;
    }

    public String getName() {
        return name;
    }

    public AirportStatus getStatus() {
        return status;
    }

    public AirportLocation getAirportLocation() {
        return airportLocation;
    }

    public Facilities getFacilities() {
        return facilities;
    }

    public Double getRouteDistance() {
        return routeDistance;
    }

    public List<Runway> getAirportRunways() {
        return airportRunways;
    }

    public List<Certification> getCertifications() {
        return certifications;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public List<String> getAirportPhotos() {
        return airportPhotos;
    }

    public void addCertification(Certification certification) {
        certifications.add(certification);
        certification.setAirport(this);
    }

    public void changeStatus(AirportStatus status) {
        this.status = status;
    }

    public void updateDetails(String name, String operationalHours,
            List<Contact> contacts) {
        this.name = name;
        this.contacts = contacts == null ? new ArrayList<>() : new ArrayList<>(contacts);
    }

}
