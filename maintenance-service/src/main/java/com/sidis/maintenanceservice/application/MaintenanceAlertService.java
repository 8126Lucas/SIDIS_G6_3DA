package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftClientDTO;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@UseCase
public class MaintenanceAlertService {

    private final AircraftRestClientService aircraftClient;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceAlertService(AircraftRestClientService aircraftClient, MaintenanceRecordRepository maintenanceRecordRepository) {
        this.aircraftClient = aircraftClient;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public record MaintenanceAlert(
            String registrationNumber,
            String aircraftModel,
            Double totalFlightHours,
            LocalDate lastMaintenanceDate,
            Long daysSinceLastMaintenance,
            String alertReason
    ) {}

    public List<MaintenanceAlert> findAircraftDueForMaintenance(int calendarDaysThreshold,
                                                                double flightHoursThreshold) {
        List<MaintenanceRecord> completedRecords = maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED);
        List<AircraftClientDTO> allAircraft = aircraftClient.findAll();
        List<MaintenanceAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (AircraftClientDTO aircraft : allAircraft) {
            Optional<MaintenanceRecord> lastMaintenance = completedRecords.stream()
                    .filter(r -> r.getAircraftRegistration()
                            .equals(aircraft.aircraftRegistration()))
                    .max(Comparator.comparing(MaintenanceRecord::getEndDate));

            List<String> reasons = new ArrayList<>();

            if (lastMaintenance.isEmpty()) {
                reasons.add("No completed maintenance on record");
            } else {
                long daysSince = ChronoUnit.DAYS.between(lastMaintenance.get().getEndDate(), today);
                if (daysSince >= calendarDaysThreshold) {
                    reasons.add("Last maintenance was " + daysSince + " days ago (threshold: " + calendarDaysThreshold + ")");
                }
            }

            if (aircraft.totalFlightHours() >= flightHoursThreshold) {
                reasons.add("Total flight hours " + aircraft.totalFlightHours() + " reached threshold of " + flightHoursThreshold);
            }

            if (!reasons.isEmpty()) {
                LocalDate lastDate = lastMaintenance.map(MaintenanceRecord::getEndDate).orElse(null);
                Long daysSince = lastDate != null ? ChronoUnit.DAYS.between(lastDate, today) : null;

                alerts.add(new MaintenanceAlert(
                        aircraft.aircraftRegistration(),
                        aircraft.modelName(),
                        aircraft.totalFlightHours(),
                        lastDate,
                        daysSince,
                        String.join("; ", reasons)
                ));
            }
        }

        return alerts;
    }
}