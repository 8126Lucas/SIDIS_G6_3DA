package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class MaintenanceReportService {

    private final AircraftRestClientService aircraftClient;
    private final MaintenanceRecordRepository maintenanceRecordRepository;


    public MaintenanceReportService(AircraftRestClientService aircraftClient, MaintenanceRecordRepository maintenanceRecordRepository) {
        this.aircraftClient = aircraftClient;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public record TurnaroundAverage(String aircraftType, Double averageDays) {}
    public record MaintenanceCostReport(String group, Double totalCost) {}

    public List<MaintenanceRecord> ongoingMaintenanceActivities() {
        return maintenanceRecordRepository.findByStatus(MaintenanceStatus.ONGOING);
    }

    public List<MaintenanceCostReport> maintenanceCosts(String groupBy) {
        Map<String, Double> totals = maintenanceRecordRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        record -> reportGroup(record, groupBy),
                        Collectors.summingDouble(this::recordCost)
                ));

        return totals.entrySet()
                .stream()
                .map(e -> new MaintenanceCostReport(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(MaintenanceCostReport::group))
                .toList();
    }

    public List<TurnaroundAverage> averageTurnaroundTimePerAircraftType() {
        List<MaintenanceRecord> completed = maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED);

        Map<String, List<Long>> daysPerType = completed.stream()
                .filter(r -> r.getEndDate() != null)
                .collect(Collectors.groupingBy(
                        r -> aircraftClient.findAircraftModelNameByRegistrationNumber(r.getAircraftRegistration()),
                        Collectors.mapping(
                                r -> ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()),
                                Collectors.toList()
                        )
                ));

        return daysPerType.entrySet().stream()
                .map(e -> new TurnaroundAverage(
                        e.getKey(),
                        e.getValue().stream().mapToLong(l -> l).average().orElse(0.0)
                ))
                .sorted(Comparator.comparing(TurnaroundAverage::aircraftType))
                .toList();
    }

    private String reportGroup(MaintenanceRecord record, String groupBy) {
        if ("model".equalsIgnoreCase(groupBy)) {
            return aircraftClient.findAircraftModelNameByRegistrationNumber(record.getAircraftRegistration());
        }
        return record.getAircraftRegistration();
    }

    private Double recordCost(MaintenanceRecord record) {
        return record.getUsedParts()
                .stream()
                .mapToDouble(part -> part.getQuantity() * part.getPrice())
                .sum();
    }
}
