package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordCommandRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.exceptions.DomainException;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceTemplateSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.RabbitMQConfig;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UseCase
@Transactional
public class MaintenanceRecordProgressService {
    private final MaintenanceRecordCommandRepository maintenanceRecordRepository;
    private final RabbitTemplate rabbitTemplate;

    public MaintenanceRecordProgressService(MaintenanceRecordCommandRepository maintenanceRecordRepository,
                                            RabbitTemplate rabbitTemplate) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        maintenanceRecordRepository.save(record);

        sendRabbitMqUpdate(record);

        return record;
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

        maintenanceRecordRepository.save(record);

        sendRabbitMqUpdate(record);

        return record;
    }

    public MaintenanceRecord completeRecord(Long recordId, String notes,
                                            Map<String, Boolean> tasksDone, LocalDate endDate) {
        MaintenanceRecord record = findRecord(recordId);
        record.markAsCompleted(notes, tasksDone, endDate);
        maintenanceRecordRepository.save(record);
        sendRabbitMqUpdate(record);
        return record;
    }

    public void changeRecordAttribute(MaintenanceRecord record, MaintenanceAttribute attribute) {
        record.getMaintenanceTemplate().changeAttribute(attribute);
    }

    private MaintenanceRecord findRecord(Long recordId) {
        return maintenanceRecordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new DomainException("Maintenance record does not exist"));
    }

    private void sendRabbitMqUpdate(MaintenanceRecord record) {
        List<UsedPartRedis> parts = new ArrayList<>();
        for(UsedPart part : record.getUsedParts()) {
            parts.add(new UsedPartRedis(part.getPartSerialNumber(),
                    part.getQuantity(),
                    part.getPrice()));
        }

        DoneListRedis doneListRedis = new DoneListRedis(record.getDoneList().getCompletionNotes(),
                record.getDoneList().getTasksDone());

        MaintenanceRecordSyncEvent event = new MaintenanceRecordSyncEvent(
                record.getRecordId(),
                record.getAircraftRegistration(),
                record.getMaintenanceTemplate().getTemplateId(),
                record.getDescription(),
                record.getTechniciansUsername(),
                record.getSupervisorUsername(),
                record.getStartDate(),
                record.getEndDate(),
                parts,
                record.getDurationHours(),
                doneListRedis,
                record.getStatus()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAINTENANCE_EXCHANGE_NAME,
                "maintenance.event.updated",
                event
        );
    }
}