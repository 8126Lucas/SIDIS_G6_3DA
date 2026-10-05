package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.exceptions.ResourceNotFoundException;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;

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
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final AircraftRestClientService aircraftClient;

    public AddMaintenanceRecordUseCase(MaintenanceRecordRepository maintenanceRecordRepository,
                                       AircraftRestClientService aircraftClient) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.aircraftClient = aircraftClient;
    }

    public void executeShort(String aircraftRegistration, MaintenanceTemplate maintenanceTemplate, Double durationHours, String description, LocalDate startDate) {
        aircraftClient.findAircraftByRegistration(aircraftRegistration)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft does not exist in hangar"));
        maintenanceRecordRepository.save(new MaintenanceRecord(aircraftRegistration, maintenanceTemplate, durationHours, description, startDate));
    }

    public void execute(MaintenanceRecord maintenanceRecord) {
        maintenanceRecordRepository.save(maintenanceRecord);
    }
}


