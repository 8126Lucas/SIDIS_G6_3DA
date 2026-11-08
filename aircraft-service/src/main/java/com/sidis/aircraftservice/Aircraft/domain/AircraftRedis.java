package com.sidis.aircraftservice.Aircraft.domain;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.exceptions.DomainException;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RedisHash("Aircraft")
public class AircraftRedis {
    @Id
    private String registrationNumber;
    private String modelName;
    private AircraftAvailability status;
    private LocalDate manufacturingDate;
    private Double totalOperationalHours;
    private Double totalFlightHours;
    private Double meanRange;
    private List<String> features;
    private SeatingPackRedis seatingPack;
    private List<InstalledComponentRedis> components;
    private List<AircraftCertificationRedis> certifications;

    public AircraftRedis() {}

    public AircraftRedis(String registrationNumber, String modelName, AircraftAvailability status,
                         LocalDate manufacturingDate, Double totalOperationalHours,
                         Double totalFlightHours, Double meanRange, List<String> features,
                         SeatingPackRedis seatingPack, List<InstalledComponentRedis> components,
                         List<AircraftCertificationRedis> certifications) {
        this.registrationNumber = registrationNumber;
        this.modelName = modelName;
        this.status = status;
        this.manufacturingDate = manufacturingDate;
        this.totalOperationalHours = totalOperationalHours;
        this.totalFlightHours = totalFlightHours;
        this.meanRange = meanRange;
        this.features = features;
        this.seatingPack = seatingPack;
        this.components = components;
        this.certifications = certifications;
    }

    public List<InstalledComponentRedis> getComponents() { return this.components; }
    public List<AircraftCertificationRedis> getCertifications() { return this.certifications; }
    public List<String> getFeatures() { return this.features; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getModelName() { return modelName; }
    public AircraftAvailability getStatus() { return status; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public Double getTotalOperationalHours() { return totalOperationalHours; }
    public Double getTotalFlightHours() { return totalFlightHours; }
    public Double getMeanRange() { return meanRange; }
    public SeatingPackRedis getSeatingPack() { return seatingPack; }

    public void setComponents(List<InstalledComponentRedis> components) { this.components = components; }
    public void setCertifications(List<AircraftCertificationRedis> certifications) { this.certifications = certifications; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public void setFeatures(List<String> features) { this.features = features; }
    public void setStatus(AircraftAvailability status) { this.status = status; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public void setMeanRange(Double meanRange) { this.meanRange = meanRange; }
    public void setSeatingPack(SeatingPackRedis seatingPack) { this.seatingPack = seatingPack; }
    public void setTotalFlightHours(Double totalFlightHours) { this.totalFlightHours = totalFlightHours; }
    public void setTotalOperationalHours(Double totalOperationalHours) { this.totalOperationalHours = totalOperationalHours; }
}