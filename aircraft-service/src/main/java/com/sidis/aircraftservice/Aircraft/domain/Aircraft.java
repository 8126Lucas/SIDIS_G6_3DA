package com.sidis.aircraftservice.Aircraft.domain;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.exceptions.DomainException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "company_aircrafts")
public class Aircraft {
    @Id
    @Column(nullable = false, updatable = false)
    private String registrationNumber;

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private AircraftModel model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftAvailability status;

    @Column(nullable = false)
    private LocalDate manufacturingDate;

    @Column(nullable = false)
    private Double totalOperationalHours;

    @Column(nullable = false)
    private Double totalFlightHours;

    private Double meanRange;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "aircraft_features",
            joinColumns = @JoinColumn(name = "registration_number")
    )
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();

    @Embedded
    private SeatingPack seatingPack;

    @OneToMany(
            mappedBy = "aircraft",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InstalledComponent> components = new ArrayList<>();

    @OneToMany(
            mappedBy = "aircraft",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AircraftCertification> certifications = new ArrayList<>();

    protected Aircraft() {}

    public Aircraft(String registrationNumber, AircraftModel model,LocalDate manufacturingDate,
                    Double totalOperationalHours, Double totalFlightHours) {
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.manufacturingDate = manufacturingDate;
        this.totalOperationalHours = totalOperationalHours;
        this.totalFlightHours = totalFlightHours;
        this.meanRange=0.0;
        this.status = AircraftAvailability.INACTIVE;
    }

    public void sendToMaintenance() {
        this.status = AircraftAvailability.MAINTENANCE;
    }

    public void activateAircraft() {
        this.status = AircraftAvailability.AVAILABLE;
    }

    public void retireAircraft() {
        this.status = AircraftAvailability.INACTIVE;
    }

    public void sendAircraftInFlight() {
        this.status = AircraftAvailability.IN_FLIGHT;
    }

    public void installComponent(InstalledComponent component) {
        if (component == null) {
            throw new IllegalArgumentException(
                    "Component cannot be null"
            );
        }
        boolean alreadyInstalled = components.stream()
                .anyMatch(c ->
                        c.getSerialNumber()
                                .equals(component.getSerialNumber())
                );
        if (alreadyInstalled) {
            throw new IllegalStateException(
                    "Component already installed"
            );
        }
        components.add(component);
        component.setAircraft(this);
    }

    public void removeComponent(InstalledComponent component) {
        if (component == null) {
            throw new IllegalArgumentException(
                    "Component cannot be null"
            );
        }
        components.remove(component);
        component.setAircraft(null);
    }

    public void addCertification(AircraftCertification certification) {
        if (certification == null) {
            throw new IllegalArgumentException(
                    "Certification cannot be null"
            );
        }
        certifications.add(certification);
        certification.setAircraft(this);
    }

    public void configureSeating(SeatingPack seatingPack) {
        if (seatingPack == null) {
            throw new IllegalArgumentException(
                    "Seating configuration cannot be null"
            );
        }
        this.seatingPack = seatingPack;
    }

    public void addFeature(String feature) {
        if (feature == null || feature.isBlank()) {
            throw new IllegalArgumentException(
                    "Feature cannot be empty"
            );
        }
        if (!features.contains(feature)) {
            features.add(feature);
        }
    }

    public void updateOperationalHours(Double additionalHours) {
        if (additionalHours == null || additionalHours <= 0) {
            throw new IllegalArgumentException(
                    "Hours must be positive"
            );
        }
        this.totalOperationalHours += additionalHours;
    }

    public void updateFlightHours(Double additionalHours) {
        if (additionalHours == null || additionalHours <= 0) {
            throw new IllegalArgumentException(
                    "Hours must be positive"
            );
        }

        this.totalFlightHours += additionalHours;
    }

    public boolean isReadyForFlight() {
        return status == AircraftAvailability.AVAILABLE
                && certifications.stream()
                .allMatch(AircraftCertification::isValid);
    }

    public List<InstalledComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public List<AircraftCertification> getCertifications() {
        return Collections.unmodifiableList(certifications);
    }

    public List<String> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public AircraftModel getModel() {
        return model;
    }

    public AircraftAvailability getStatus() {
        return status;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public Double getTotalOperationalHours() {
        return totalOperationalHours;
    }

    public Double getTotalFlightHours() {
        return totalFlightHours;
    }

    public Double getMeanRange() {
        return meanRange;
    }

    public SeatingPack getSeatingPack() {
        return seatingPack;
    }

    public void updateMeanRange(Double newMeanRange) {
        if(meanRange!=null){
            this.meanRange += newMeanRange;
        }else{
            throw new IllegalStateException(
                    "Range to update can not be null"
            );
        }
    }
}