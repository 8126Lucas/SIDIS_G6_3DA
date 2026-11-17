package com.sidis.maintenanceservice;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordCommandRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceTemplateSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
//@Profile("dev")
public class Bootstrap implements ApplicationRunner {
    private final MaintenanceRecordCommandRepository maintenanceRecordRepository;
    private final MaintenanceTemplateCommandRepository maintenanceTemplateRepository;
    private final RabbitTemplate rabbitTemplate;

    public Bootstrap(MaintenanceRecordCommandRepository maintenanceRecordRepository,
                     MaintenanceTemplateCommandRepository maintenanceTemplateRepository,
                     RabbitTemplate rabbitTemplate) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (maintenanceRecordRepository.count() == 0 && maintenanceTemplateRepository.count() == 0) {
            seedMaintenance();
        }
    }

    private void seedMaintenance() {
        // ================= ENGINE INSPECTION =================

        MaintenanceTemplate engineInspection = new MaintenanceTemplate(
                "ENGINE-INSPECTION",
                20.0,
                Map.of(
                        "Check oil", true,
                        "Inspect motor", true,
                        "Verify pressure systems", true
                ),
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.ENGINE
        );

        maintenanceTemplateRepository.save(engineInspection);

        MaintenanceRecord r1 = new MaintenanceRecord(
                List.of(
                        new UsedPart("ENG-001", 2, 1500.0)
                ),
                24.0,
                LocalDate.now().minusDays(10),
                "supervisor",
                "Completed",
                List.of("tech"),
                engineInspection,
                "CS-TUA"
        );

        r1.markAsCompleted(new DoneList(
                "Engine inspection completed successfully.",
                Map.of(
                        "Check oil", true,
                        "Inspect motor", true,
                        "Verify pressure systems", true
                )),LocalDate.now().minusDays(1));

        // ================= LANDING GEAR =================

        MaintenanceTemplate landingGearInspection = new MaintenanceTemplate(
                "LANDING-GEAR-CHECK",
                12.0,
                Map.of(
                        "Inspect tires", true,
                        "Check brake assemblies", true,
                        "Verify hydraulic actuators", true
                ),
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.AIRFRAME
        );

        maintenanceTemplateRepository.save(landingGearInspection);

        MaintenanceRecord r2 = new MaintenanceRecord(
                List.of(
                        new UsedPart("TIRE-900", 4, 800.0),
                        new UsedPart("BRAKE-200", 2, 1200.0)
                ),
                15.0,
                LocalDate.now().minusDays(5),
                "supervisor",
                "Completed",
                List.of("tech"),
                landingGearInspection,
                "D-ABXA"
        );
        r2.markAsCompleted(new DoneList(
                "Landing gear inspection completed. Tires replaced.",
                Map.of(
                        "Inspect tires", true,
                        "Check brake assemblies", true,
                        "Verify hydraulic actuators", true
                )
        ),LocalDate.now().minusDays(1));

        // ================= AVIONICS =================

        MaintenanceTemplate avionicsCheck = new MaintenanceTemplate(
                "AVIONICS-DIAGNOSTIC",
                8.0,
                Map.of(
                        "Run diagnostics", true,
                        "Inspect flight computer", true,
                        "Verify communication systems", true
                ),
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.AVIONICS
        );

        maintenanceTemplateRepository.save(avionicsCheck);

        MaintenanceRecord r3 = new MaintenanceRecord(
                List.of(),
                8.0,
                LocalDate.now().minusDays(2),
                "supervisor",
                "Completed",
                List.of("tech"),
                avionicsCheck,
                "F-HXKA"
        );

        r3.markAsCompleted(new DoneList(
                "Avionics diagnostic completed with no issues.",
                Map.of(
                        "Run diagnostics", true,
                        "Inspect flight computer", true,
                        "Verify communication systems", true
                )
        ),LocalDate.now().minusDays(1));

        // ================= ENGINE REPAIR =================

        MaintenanceTemplate engineRepair = new MaintenanceTemplate(
                "ENGINE-REPAIR",
                40.0,
                Map.of(
                        "Replace fuel pump", true,
                        "Replace oil filter", true,
                        "Ground engine test", true
                ),
                MaintenanceType.OVERHAUL,
                MaintenanceAttribute.ENGINE
        );

        maintenanceTemplateRepository.save(engineRepair);

        MaintenanceRecord r4 = new MaintenanceRecord(
                List.of(
                        new UsedPart("FUEL-PUMP-01", 1, 3200.0),
                        new UsedPart("OIL-FILTER-22", 2, 250.0)
                ),
                40.0,
                LocalDate.now(),
                "supervisor",
                "In Progress",
                List.of("tech"),
                engineRepair,
                "CS-TUA"
        );

        List<MaintenanceTemplate> maintenanceTemplates = List.of(engineInspection, landingGearInspection, avionicsCheck, engineRepair);
        List<MaintenanceRecord> maintenanceRecords = List.of(r1, r2, r3, r4);

        maintenanceRecordRepository.saveAll(maintenanceRecords);

        for (MaintenanceTemplate template : maintenanceTemplates) {
            MaintenanceTemplateSyncEvent event = new MaintenanceTemplateSyncEvent(
                    template.getTemplateId(),
                    template.getName(),
                    template.getExpectedDuration(),
                    template.getTemplateChecklist(),
                    template.getOperation(),
                    template.getAttribute()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TEMPLATE_EXCHANGE_NAME,
                    "maintenancetemplate.event.created",
                    event
            );
        }

        for (MaintenanceRecord record : maintenanceRecords) {
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
    }
}