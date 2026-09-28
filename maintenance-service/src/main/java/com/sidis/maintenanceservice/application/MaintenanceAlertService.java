package com.sidis.maintenanceservice.application;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.maintenanceservice.UseCase;

@UseCase
public class MaintenanceAlertService {

    /*private final AircraftRepository aircraftRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceAlertService(AircraftRepository aircraftRepository,
                                   MaintenanceRecordRepository maintenanceRecordRepository) {
        this.aircraftRepository = aircraftRepository;
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
        List<Aircraft> allAircraft = aircraftRepository.findAll();
        List<MaintenanceAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Aircraft aircraft : allAircraft) {
            Optional<MaintenanceRecord> lastMaintenance = completedRecords.stream()
                    .filter(r -> r.getAircraft().getRegistrationNumber()
                            .equals(aircraft.getRegistrationNumber()))
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

            if (aircraft.getTotalFlightHours() >= flightHoursThreshold) {
                reasons.add("Total flight hours " + aircraft.getTotalFlightHours() + " reached threshold of " + flightHoursThreshold);
            }

            if (!reasons.isEmpty()) {
                LocalDate lastDate = lastMaintenance.map(MaintenanceRecord::getEndDate).orElse(null);
                Long daysSince = lastDate != null ? ChronoUnit.DAYS.between(lastDate, today) : null;

                alerts.add(new MaintenanceAlert(
                        aircraft.getRegistrationNumber(),
                        aircraft.getModel().getModelName(),
                        aircraft.getTotalFlightHours(),
                        lastDate,
                        daysSince,
                        String.join("; ", reasons)
                ));
            }
        }

        return alerts;
    }*/
}