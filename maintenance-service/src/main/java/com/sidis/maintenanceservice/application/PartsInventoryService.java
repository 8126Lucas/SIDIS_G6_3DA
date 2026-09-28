package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.UsedPart;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.UseCase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class PartsInventoryService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public PartsInventoryService(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public record PartUsageSummary(
            String partSerialNumber,
            Integer totalQuantityUsed,
            Double totalCost,
            Long timesUsedInRecords
    ) {}

    public record LowStockAlert(
            String partSerialNumber,
            Integer totalQuantityUsed,
            Long timesUsedInRecords,
            String alertMessage
    ) {}

    public List<PartUsageSummary> getPartsUsageSummary() {
        Map<String, List<UsedPart>> bySerial = maintenanceRecordRepository.findAll()
                .stream()
                .flatMap(r -> r.getUsedParts().stream())
                .collect(Collectors.groupingBy(UsedPart::getPartSerialNumber));

        return bySerial.entrySet().stream()
                .map(e -> {
                    List<UsedPart> parts = e.getValue();
                    int totalQty = parts.stream().mapToInt(UsedPart::getQuantity).sum();
                    double totalCost = parts.stream()
                            .mapToDouble(p -> p.getQuantity() * p.getPrice())
                            .sum();
                    return new PartUsageSummary(e.getKey(), totalQty, totalCost, (long) parts.size());
                })
                .sorted(Comparator.comparing(PartUsageSummary::partSerialNumber))
                .toList();
    }

    public List<LowStockAlert> getLowStockAlerts(int threshold) {
        return getPartsUsageSummary().stream()
                .filter(s -> s.totalQuantityUsed() >= threshold)
                .map(s -> new LowStockAlert(
                        s.partSerialNumber(),
                        s.totalQuantityUsed(),
                        s.timesUsedInRecords(),
                        "Part " + s.partSerialNumber() + " used " + s.totalQuantityUsed()
                                + " units across " + s.timesUsedInRecords()
                                + " record(s); consider restocking (threshold: " + threshold + ")"
                ))
                .toList();
    }
}
