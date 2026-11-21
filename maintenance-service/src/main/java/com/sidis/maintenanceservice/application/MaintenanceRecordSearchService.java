package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class MaintenanceRecordSearchService {
    private final MaintenanceRecordQueryRepository maintenanceRecordRepository;
    private final MaintenanceTemplateSearchService templateSearchService;

    public MaintenanceRecordSearchService(MaintenanceRecordQueryRepository maintenanceRecordRepository,
                                          MaintenanceTemplateSearchService templateSearchService) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.templateSearchService = templateSearchService;
    }

    public Page<MaintenanceRecordRedis> findRecordsMatchingFilter(
            String aircraftRegistration,
            String attribute,
            LocalDate initialDate,
            LocalDate finalDate,
            Pageable pageable) {

        List<MaintenanceRecordRedis> allRecords = maintenanceRecordRepository.findAll();

        List<MaintenanceRecordRedis> filtered = allRecords.stream()
                .filter(r -> aircraftRegistration == null || aircraftRegistration.isEmpty() || aircraftRegistration.equals(r.getAircraftRegistration()))
                .filter(r -> {
                    if (attribute == null || attribute.isBlank()) return true;
                    return templateSearchService.findByTemplateId(r.getMaintenanceTemplateId())
                            .map(t -> t.getAttribute().name().equals(attribute))
                            .orElse(false);
                })
                .filter(r -> initialDate == null || !r.getStartDate().isBefore(initialDate))
                .filter(r -> finalDate == null || !r.getStartDate().isAfter(finalDate))
                .sorted(Comparator.comparing(MaintenanceRecordRedis::getRecordId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<MaintenanceRecordRedis> pageContent = (start <= end && start < filtered.size()) ? filtered.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }
}