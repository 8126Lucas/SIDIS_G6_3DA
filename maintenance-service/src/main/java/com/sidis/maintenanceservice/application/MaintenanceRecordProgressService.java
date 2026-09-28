package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.UsedPart;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.exceptions.DomainException;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UseCase
@Transactional
public class MaintenanceRecordProgressService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceRecordProgressService(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public Optional<MaintenanceRecord> findById(Long recordId) {
        return maintenanceRecordRepository.findByRecordId(recordId);
    }

    public MaintenanceRecord advanceStatus(Long recordId, MaintenanceStatus newStatus) {
        MaintenanceRecord record = findRecord(recordId);
        switch (newStatus) {
            case ONGOING -> record.markAsOngoing();
            case COMPLETED -> throw new DomainException(
                    "Use the complete endpoint to mark as completed");
            default -> throw new DomainException("Invalid status transition");
        }
        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord updateProgress(Long recordId, Double realDuration,
                                            List<UsedPart> usedParts) {
        MaintenanceRecord record = findRecord(recordId);
        record.markAsOngoing();

        if (realDuration != null) {
            record.updateDuration(realDuration);
        }

        if (usedParts != null) {
            usedParts.forEach(record::addUsedPart);
        }

        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord completeRecord(Long recordId, String notes,
                                            Map<String, Boolean> tasksDone, LocalDate endDate) {
        MaintenanceRecord record = findRecord(recordId);
        record.markAsCompleted(notes, tasksDone, endDate);
        return maintenanceRecordRepository.save(record);
    }

    public void changeRecordAttribute(MaintenanceRecord record, MaintenanceAttribute attribute) {
        record.getMaintenanceTemplate().changeAttribute(attribute);
    }

    private MaintenanceRecord findRecord(Long recordId) {
        return maintenanceRecordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new DomainException("Maintenance record does not exist"));
    }
}