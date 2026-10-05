package com.sidis.maintenanceservice.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "company_maintenance_records")
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Version
    private Long version;

    @Column(name = "aircraft_id")
    private String aircraftRegistration;

    @ManyToOne
    @JoinColumn(name = "maintenance_template_id")
    private MaintenanceTemplate maintenanceTemplate;

    @Column(nullable = false)
    private String description;

    @Column
    private List<String> techniciansUsername = new ArrayList<>();

    @Column(name = "supervisor_id")
    private String supervisorUsername;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @ElementCollection
    @CollectionTable(name = "maintenance_used_parts")
    private List<UsedPart> usedParts = new ArrayList<>();

    @Column(nullable = false)
    private Double durationHours;

    @Embedded
    private DoneList doneList;

    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status;

    public MaintenanceRecord() {
    }

    public MaintenanceRecord(String aircraftRegistration, MaintenanceTemplate maintenanceTemplate,
                             Double durationHours, String description, LocalDate startDate){
        this.aircraftRegistration = aircraftRegistration;
        this.maintenanceTemplate = maintenanceTemplate;
        this.durationHours = durationHours;
        this.description = description;
        this.startDate = startDate;
        this.status = MaintenanceStatus.INLINE;
    }

    public MaintenanceRecord(List<UsedPart> usedParts,Double durationHours,
            LocalDate startDate,String supervisorUsername,String description,
            List<String> techniciansUsername, MaintenanceTemplate maintenanceTemplate,
            String aircraftRegistration) {
        this.status = MaintenanceStatus.INLINE;
        this.usedParts = usedParts != null ? usedParts : new ArrayList<>();
        this.durationHours = durationHours;
        this.startDate = startDate;
        this.supervisorUsername = supervisorUsername;
        this.description = description;
        this.techniciansUsername = techniciansUsername != null ? techniciansUsername : new ArrayList<>();
        this.maintenanceTemplate = maintenanceTemplate;
        this.aircraftRegistration = aircraftRegistration;
    }

    public void updateDuration(Double realDuration) {
        if (realDuration == null || realDuration <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be positive"
            );
        }

        this.durationHours = realDuration;
    }

    public void assignTechnician(String technicianUsername) {
        if (technicianUsername == null) {
            throw new IllegalArgumentException(
                    "Technician cannot be null"
            );
        }

        if (!techniciansUsername.contains(technicianUsername)) {
            techniciansUsername.add(technicianUsername);
        }
    }

    public void addUsedPart(UsedPart part) {
        if (part == null) {
            throw new IllegalArgumentException(
                    "Used part cannot be null"
            );
        }

        usedParts.add(part);
    }

    public void markAsOngoing(){
        this.status = MaintenanceStatus.ONGOING;
    }

    public void markAsCompleted(String notes,Map<String, Boolean> tasksDone,LocalDate endDate) {
        if (this.status == MaintenanceStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Maintenance record already completed"
            );
        }

        if (endDate == null) {
            throw new IllegalArgumentException(
                    "End date cannot be null"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        this.status = MaintenanceStatus.COMPLETED;
        this.doneList = new DoneList(notes, tasksDone);
        this.endDate = endDate;
    }

    public void markAsCompleted(DoneList doneList,LocalDate endDate) {
        this.status = MaintenanceStatus.COMPLETED;
        this.doneList = doneList;
        this.endDate = endDate;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getAircraftRegistration() {
        return aircraftRegistration;
    }

    public MaintenanceTemplate getMaintenanceTemplate() {
        return maintenanceTemplate;
    }

    public List<String> getTechniciansUsername() {
        return techniciansUsername;
    }

    public String getSupervisorUsername() {
        return supervisorUsername;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<UsedPart> getUsedParts() {
        return usedParts;
    }

    public Double getDurationHours() {
        return durationHours;
    }

    public DoneList getDoneList() {
        return doneList;
    }

    public void updateDoneList(DoneList list){
        this.doneList = list;
    }
}
