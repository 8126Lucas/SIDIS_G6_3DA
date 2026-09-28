package com.sidis.maintenanceservice;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
//@Profile("dev")
public class Bootstrap implements ApplicationRunner {
    
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;

    public Bootstrap(MaintenanceRecordRepository maintenanceRecordRepository,
                     MaintenanceTemplateRepository maintenanceTemplateRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (maintenanceRecordRepository.count() == 0) {
            seedMaintenance();
        }
    }

    private void seedMaintenance() {
        /*Collaborator supervisor = collaboratorRepository
                .findByUsername("supervisor")
                .orElseThrow();

        Collaborator tech = collaboratorRepository
                .findByUsername("technician")
                .orElseThrow();

        Aircraft csTua = aircraftRepository
                .findByRegistrationNumber("CS-TUA")
                .orElseThrow();

        Aircraft dAbxa = aircraftRepository
                .findByRegistrationNumber("D-ABXA")
                .orElseThrow();

        Aircraft fHxka = aircraftRepository
                .findByRegistrationNumber("F-HXKA")
                .orElseThrow();*/

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
                /*supervisor,*/
                "Completed",
                /*List.of(tech),*/
                engineInspection/*,
                csTua*/
        );

        r1.markAsCompleted(new DoneList(
                "Engine inspection completed successfully.",
                Map.of(
                        "Check oil", true,
                        "Inspect motor", true,
                        "Verify pressure systems", true
                )),     LocalDate.of(2003, 3, 1));

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
                /*supervisor,*/
                "Completed",
                /*List.of(tech),*/
                landingGearInspection/*,
                dAbxa*/
        );
        r2.markAsCompleted(new DoneList(
                "Landing gear inspection completed. Tires replaced.",
                Map.of(
                        "Inspect tires", true,
                        "Check brake assemblies", true,
                        "Verify hydraulic actuators", true
                )
        ),LocalDate.of(2013, 4, 12));

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
                /*supervisor,*/
                "Completed",
                /*List.of(tech),*/
                avionicsCheck/*,
                fHxka*/
        );

        r3.markAsCompleted(new DoneList(
                "Avionics diagnostic completed with no issues.",
                Map.of(
                        "Run diagnostics", true,
                        "Inspect flight computer", true,
                        "Verify communication systems", true
                )
        ),LocalDate.of(2023, 9, 21));

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
                /*supervisor,*/
                "In Progress",
                /*List.of(tech),*/
                engineRepair/*,
                csTua*/
        );

        maintenanceRecordRepository.saveAll(
                List.of(r1, r2, r3, r4)
        );
    }
}