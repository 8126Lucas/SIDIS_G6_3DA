package com.sidis.maintenanceservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RedisHash("MaintenanceRecord")
public class MaintenanceRecordRedis {
    @Id
    private Long recordId;
    private String aircraftRegistration;
    private Long maintenanceTemplateId;
    private String description;
    private List<String> techniciansUsername;
    private String supervisorUsername;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<UsedPartRedis> usedParts;
    private Double durationHours;
    private DoneListRedis doneList;
    private MaintenanceStatus status;

    public MaintenanceRecordRedis() {
    }

    public MaintenanceRecordRedis(String aircraftRegistration, Long maintenanceTemplateId,
                                  Double durationHours, String description, LocalDate startDate){
        this.aircraftRegistration = aircraftRegistration;
        this.maintenanceTemplateId = maintenanceTemplateId;
        this.durationHours = durationHours;
        this.description = description;
        this.startDate = startDate;
        this.status = MaintenanceStatus.INLINE;
    }

    public MaintenanceRecordRedis(List<UsedPartRedis> usedParts, Double durationHours,
                                  LocalDate startDate, String supervisorUsername, String description,
                                  List<String> techniciansUsername, Long maintenanceTemplateId,
                                  String aircraftRegistration) {
        this.status = MaintenanceStatus.INLINE;
        this.usedParts = usedParts != null ? usedParts : new ArrayList<>();
        this.durationHours = durationHours;
        this.startDate = startDate;
        this.supervisorUsername = supervisorUsername;
        this.description = description;
        this.techniciansUsername = techniciansUsername != null ? techniciansUsername : new ArrayList<>();
        this.maintenanceTemplateId = maintenanceTemplateId;
        this.aircraftRegistration = aircraftRegistration;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getAircraftRegistration() {
        return aircraftRegistration;
    }
    public void setAircraftRegistration(String aircraftRegistration) { this.aircraftRegistration = aircraftRegistration; }

    public Long getMaintenanceTemplateId() {
        return maintenanceTemplateId;
    }
    public void setMaintenanceTemplateId(Long maintenanceTemplateId) { this.maintenanceTemplateId = maintenanceTemplateId; }

    public List<String> getTechniciansUsername() {
        return techniciansUsername;
    }
    public void setTechniciansUsername(List<String> techniciansUsername) { this.techniciansUsername = techniciansUsername; }

    public String getSupervisorUsername() {
        return supervisorUsername;
    }
    public void setSupervisorUsername(String supervisorUsername) { this.supervisorUsername = supervisorUsername; }

    public MaintenanceStatus getStatus() {
        return status;
    }
    public void setStatus(MaintenanceStatus status) { this.status = status; }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<UsedPartRedis> getUsedParts() {
        return usedParts;
    }
    public void setUsedParts(List<UsedPartRedis> usedParts) { this.usedParts = usedParts; }

    public Double getDurationHours() {
        return durationHours;
    }
    public void setDurationHours(Double durationHours) { this.durationHours = durationHours; }

    public DoneListRedis getDoneList() {
        return doneList;
    }
    public void setDoneList(DoneListRedis list){
        this.doneList = list;
    }

}
