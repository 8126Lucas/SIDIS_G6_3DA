package com.sidis.maintenanceservice.domain;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.Users.domain.Collaborator;
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

    /*@ManyToOne(optional = false)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;*/

    @ManyToOne
    @JoinColumn(name = "maintenance_template_id")
    private MaintenanceTemplate maintenanceTemplate;

    @Column(nullable = false)
    private String description;

    /*@ManyToMany
    @JoinTable(
            name = "maintenance_technicians",
            joinColumns = @JoinColumn(name = "maintenance_record_id"),
            inverseJoinColumns = @JoinColumn(name = "technician_id")
    )
    private List<Collaborator> technicians = new ArrayList<>();*/

    /*@ManyToOne
    @JoinColumn(name = "supervisor_id")
    private Collaborator supervisor;*/

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

    public MaintenanceRecord(/*Aircraft aircraft,*/MaintenanceTemplate maintenanceTemplate,
                             Double durationHours, String description, LocalDate startDate){
        /*this.aircraft = aircraft;*/
        this.maintenanceTemplate = maintenanceTemplate;
        this.durationHours = durationHours;
        this.description = description;
        this.startDate = startDate;
        this.status = MaintenanceStatus.INLINE;
    }

    public MaintenanceRecord(List<UsedPart> usedParts,Double durationHours,
            LocalDate startDate,/*Collaborator supervisor,*/String description,
            /*List<Collaborator> technicians,*/MaintenanceTemplate maintenanceTemplate/*,
            Aircraft aircraft*/) {
        this.status = MaintenanceStatus.INLINE;
        this.usedParts = usedParts != null ? usedParts : new ArrayList<>();
        this.durationHours = durationHours;
        this.startDate = startDate;
//        this.supervisor = supervisor;
        this.description = description;
//        this.technicians = technicians != null ? technicians : new ArrayList<>();
        this.maintenanceTemplate = maintenanceTemplate;
//        this.aircraft = aircraft;
    }

    public void updateDuration(Double realDuration) {
        if (realDuration == null || realDuration <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be positive"
            );
        }

        this.durationHours = realDuration;
    }

    /*public void assignTechnician(Collaborator technician) {
        if (technician == null) {
            throw new IllegalArgumentException(
                    "Technician cannot be null"
            );
        }

        if (!technicians.contains(technician)) {
            technicians.add(technician);
        }
    }*/

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

    /*public Aircraft getAircraft() {
        return aircraft;
    }*/

    public MaintenanceTemplate getMaintenanceTemplate() {
        return maintenanceTemplate;
    }

    /*public List<Collaborator> getTechnicians() {
        return technicians;
    }*/

    /*public Collaborator getSupervisor() {
        return supervisor;
    }*/

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
