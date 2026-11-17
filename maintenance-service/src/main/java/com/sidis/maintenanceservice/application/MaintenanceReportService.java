package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceHoursReport;
import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.MaintenanceHoursRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class MaintenanceReportService {

    private final AircraftRestClientService aircraftClient;
    private final MaintenanceRecordQueryRepository maintenanceRecordRepository;
    private final MaintenanceHoursRepository reportRedisRepository;


    public MaintenanceReportService(AircraftRestClientService aircraftClient,
                                    MaintenanceRecordQueryRepository maintenanceRecordRepository,
                                    MaintenanceHoursRepository reportRedisRepository) {
        this.aircraftClient = aircraftClient;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.reportRedisRepository = reportRedisRepository;
    }

    public record TurnaroundAverage(String aircraftType, Double averageDays) {}
    public record MaintenanceCostReport(String group, Double totalCost) {}

    public List<MaintenanceRecordRedis> ongoingMaintenanceActivities() {
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
        List<MaintenanceRecordRedis> completed = maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED);

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

    private String reportGroup(MaintenanceRecordRedis record, String groupBy) {
        if ("model".equalsIgnoreCase(groupBy)) {
            return aircraftClient.findAircraftModelNameByRegistrationNumber(record.getAircraftRegistration());
        }
        return record.getAircraftRegistration();
    }

    private Double recordCost(MaintenanceRecordRedis record) {
        if (record.getUsedParts() == null) return 0.0;

        return record.getUsedParts()
                .stream()
                .mapToDouble(part -> part.getQuantity() * part.getPrice())
                .sum();
    }

    public List<MaintenanceHoursReport> totalMaintenanceHoursByAircraft() {
        List<MaintenanceHoursReport> result = new ArrayList<>();

        reportRedisRepository.findAll().forEach(report -> {
            result.add(new MaintenanceHoursReport(
                    report.getAircraftRegistration(),
                    report.getTotalMaintenanceHours()
            ));
        });

        result.sort(Comparator.comparing(MaintenanceHoursReport::aircraftRegistration));

        return result;
    }
}
