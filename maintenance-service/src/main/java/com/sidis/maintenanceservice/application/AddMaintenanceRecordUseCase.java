package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.exceptions.DomainException;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordCommandRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


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
    private final RabbitTemplate rabbitTemplate;

    public AddMaintenanceRecordUseCase(MaintenanceRecordCommandRepository maintenanceRecordRepository,
                                       MaintenanceTemplateCommandRepository maintenanceTemplateCommandRepository,
                                       AircraftRestClientService aircraftClient,
                                       RabbitTemplate rabbitTemplate) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceTemplateCommandRepository = maintenanceTemplateCommandRepository;
        this.aircraftClient = aircraftClient;
        this.rabbitTemplate = rabbitTemplate;
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
                "maintenance.event.created",
                event
        );

    }

    public void execute(MaintenanceRecord maintenanceRecord) {
        maintenanceRecordRepository.save(maintenanceRecord);

        List<UsedPartRedis> parts = new ArrayList<>();
        for(UsedPart part : maintenanceRecord.getUsedParts()) {
            parts.add(new UsedPartRedis(part.getPartSerialNumber(),
                    part.getQuantity(),
                    part.getPrice()));
        }

        DoneListRedis doneListRedis = new DoneListRedis(maintenanceRecord.getDoneList().getCompletionNotes(),
                maintenanceRecord.getDoneList().getTasksDone());

        MaintenanceRecordSyncEvent event = new MaintenanceRecordSyncEvent(
                maintenanceRecord.getRecordId(),
                maintenanceRecord.getAircraftRegistration(),
                maintenanceRecord.getMaintenanceTemplate().getTemplateId(),
                maintenanceRecord.getDescription(),
                maintenanceRecord.getTechniciansUsername(),
                maintenanceRecord.getSupervisorUsername(),
                maintenanceRecord.getStartDate(),
                maintenanceRecord.getEndDate(),
                parts,
                maintenanceRecord.getDurationHours(),
                doneListRedis,
                maintenanceRecord.getStatus()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAINTENANCE_EXCHANGE_NAME,
                "maintenance.event.created",
                event
        );
    }
}


