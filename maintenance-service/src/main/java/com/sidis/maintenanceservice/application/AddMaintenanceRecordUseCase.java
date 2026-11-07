package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.exceptions.DomainException;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordCommandRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


/**
 * Use case: a Maintenance Technician adds a new maintenance record.
 *
 * <p>aircraft
 * registration, maintenance type (according to a maintenance template), description, start date, expected duration and
 * its checklist (defined by the maintenance template)
 *
 */

@UseCase
public class AddMaintenanceRecordUseCase {
    private final MaintenanceRecordCommandRepository maintenanceRecordRepository;
    private final MaintenanceTemplateCommandRepository maintenanceTemplateCommandRepository;
    private final AircraftRestClientService aircraftClient;

    public AddMaintenanceRecordUseCase(MaintenanceRecordCommandRepository maintenanceRecordRepository,
                                       MaintenanceTemplateCommandRepository maintenanceTemplateCommandRepository,
                                       AircraftRestClientService aircraftClient) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceTemplateCommandRepository = maintenanceTemplateCommandRepository;
        this.aircraftClient = aircraftClient;
    }

    @Transactional
    public void executeShort(String registrationNumber, Long templateId, Double durationHours, String description, LocalDate startDate) {
        MaintenanceTemplate template = maintenanceTemplateCommandRepository.findById(templateId)
                .orElseThrow(() -> new DomainException("Maintenance Template does not exist"));
        MaintenanceRecord record = new MaintenanceRecord(
                registrationNumber,
                template,
                durationHours,
                description,
                startDate
        );

        maintenanceRecordRepository.save(record);

    }

    public void execute(MaintenanceRecord maintenanceRecord) {
        maintenanceRecordRepository.save(maintenanceRecord);
    }
}


