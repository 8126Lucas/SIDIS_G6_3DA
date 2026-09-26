package com.sidis.aircraftservice;

import com.sidis.aircraftservice.Aircraft.domain.*;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelRepository;
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

    private final AircraftModelRepository aircraftModelRepository;
    private final AircraftRepository aircraftRepository;

    public Bootstrap(AircraftModelRepository aircraftModelRepository, AircraftRepository aircraftRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.aircraftRepository = aircraftRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (aircraftModelRepository.count() == 0) {
            seedAircraftModels();
        }
        if (aircraftRepository.count() == 0) {
            seedAircraft();
        }
    }

    private void seedAircraftModels() {
        aircraftModelRepository.saveAll(List.of(
                new AircraftModel(
                        "737-800",
                        "Boeing",
                        new AircraftSpecs(70500.0, 5436.0, 842.0, 189),
                        List.of("b737-800-front.jpg", "b737-800-side.jpg", "b737-800-cabin.jpg")
                ),

                new AircraftModel(
                        "737 MAX 8",
                        "Boeing",
                        new AircraftSpecs(82000.0, 6570.0, 839.0, 178),
                        List.of("b737-max8-1.jpg", "b737-max8-2.jpg")
                ),

                new AircraftModel(
                        "A320neo",
                        "Airbus",
                        new AircraftSpecs(79000.0, 6300.0, 828.0, 180),
                        List.of("a320neo-1.jpg", "a320neo-2.jpg", "a320neo-cockpit.jpg")
                ),

                new AircraftModel(
                        "A321neo",
                        "Airbus",
                        new AircraftSpecs(97000.0, 7400.0, 828.0, 220),
                        List.of("a321neo-1.jpg", "a321neo-2.jpg")
                ),

                new AircraftModel(
                        "A350-900",
                        "Airbus",
                        new AircraftSpecs(280000.0, 15000.0, 903.0, 300),
                        List.of("a350-900-1.jpg", "a350-900-2.jpg", "a350-interior.jpg")
                ),

                new AircraftModel(
                        "A380-800",
                        "Airbus",
                        new AircraftSpecs(575000.0, 15200.0, 903.0, 500),
                        List.of("a380-1.jpg", "a380-2.jpg", "a380-double-deck.jpg")
                ),

                new AircraftModel(
                        "787-9 Dreamliner",
                        "Boeing",
                        new AircraftSpecs(254000.0, 14140.0, 913.0, 296),
                        List.of("b787-9-1.jpg", "b787-9-cabin.jpg")
                ),

                new AircraftModel(
                        "787-10 Dreamliner",
                        "Boeing",
                        new AircraftSpecs(254000.0, 11800.0, 903.0, 330),
                        List.of("b787-10-1.jpg", "b787-10-2.jpg")
                ),

                new AircraftModel(
                        "777-300ER",
                        "Boeing",
                        new AircraftSpecs(351500.0, 13600.0, 905.0, 396),
                        List.of("b777-300er-1.jpg", "b777-300er-cabin.jpg")
                ),

                new AircraftModel(
                        "E195-E2",
                        "Embraer",
                        new AircraftSpecs(61500.0, 4815.0, 870.0, 132),
                        List.of("e195-e2-1.jpg", "e195-e2-2.jpg")
                ),

                new AircraftModel(
                        "E190-E2",
                        "Embraer",
                        new AircraftSpecs(56700.0, 4537.0, 870.0, 114),
                        List.of("e190-e2-1.jpg", "e190-e2-cabin.jpg")
                ),

                new AircraftModel(
                        "CRJ900",
                        "Bombardier",
                        new AircraftSpecs(34000.0, 2871.0, 880.0, 90),
                        List.of("crj900-1.jpg", "crj900-2.jpg")
                ),

                new AircraftModel(
                        "ATR 72-600",
                        "ATR",
                        new AircraftSpecs(23000.0, 1528.0, 510.0, 78),
                        List.of("atr72-1.jpg", "atr72-cabin.jpg")
                )
        ));
    }

    private void seedAircraft() {
        AircraftModel b737 = aircraftModelRepository
                .findByModelName("737-800")
                .orElseThrow();

        Aircraft a1 = new Aircraft("CS-TUA", b737,
                LocalDate.of(2018, 5, 10),
                12500.0,
                9800.0);

        a1.configureSeating(new SeatingPack(189, "3-3"));
        a1.addFeature("Wi-Fi");
        a1.addFeature("USB Charging");
        a1.addCertification(new AircraftCertification(
                "EASA Safety Compliance Approval",
                "SAFETY",
                LocalDate.of(2023, 6, 1),
                LocalDate.of(2026, 6, 1)
        ));
        a1.installComponent(new InstalledComponent(
                "CFM56-7B-001",
                "CFM56-7B",
                "ACTIVE",
                a1
        ));
        a1.retireAircraft();

        Aircraft a2 = new Aircraft("D-ABXA", b737,
                LocalDate.of(2016, 2, 18),
                18500.0,
                14200.0);

        a2.configureSeating(new SeatingPack(189, "3-3"));
        a2.addFeature("Wi-Fi");
        a2.addCertification(new AircraftCertification(
                "ICAO Operational Certification",
                "OPERATIONAL",
                LocalDate.of(2021, 1, 10),
                LocalDate.of(2024, 1, 10)
        ));
        a2.installComponent(new InstalledComponent(
                "CFM56-7B-002",
                "CFM56-7B",
                "ACTIVE",
                a2
        ));
        a2.activateAircraft();

        AircraftModel a320 = aircraftModelRepository
                .findByModelName("A320neo")
                .orElseThrow();

        Aircraft medium = new Aircraft(
                "TEST-002",
                a320,
                LocalDate.of(2022, 1, 1),
                1000.0,
                800.0
        );

        medium.configureSeating(new SeatingPack(180, "3-3"));

        Aircraft a3 = new Aircraft("F-HXKA", a320,
                LocalDate.of(2020, 9, 5),
                8200.0,
                6100.0);

        a3.configureSeating(new SeatingPack(180, "3-3"));
        a3.addFeature("Wi-Fi");
        a3.addFeature("In-flight Entertainment");
        a3.addCertification(new AircraftCertification(
                "EASA Airworthiness Certificate",
                "SAFETY",
                LocalDate.of(2022, 4, 12),
                LocalDate.of(2025, 4, 12)
        ));
        a3.installComponent(new InstalledComponent(
                "LEAP-1A-001",
                "LEAP-1A",
                "ACTIVE",
                a3
        ));
        a3.sendToMaintenance();

        AircraftModel a321 = aircraftModelRepository
                .findByModelName("A321neo")
                .orElseThrow();

        Aircraft a4 = new Aircraft("G-X321", a321,
                LocalDate.of(2021, 1, 20),
                5400.0,
                4100.0);

        a4.configureSeating(new SeatingPack(220, "3-3"));
        a4.addFeature("WiFi");
        a4.addFeature("Extra Legroom Rows");
        a4.installComponent(new InstalledComponent(
                "PW1100G-001",
                "PW1100G",
                "ACTIVE",
                a4
        ));

        AircraftModel b787 = aircraftModelRepository
                .findByModelName("787-9 Dreamliner")
                .orElseThrow();

        Aircraft large = new Aircraft(
                "TEST-003",
                b787,
                LocalDate.of(2022, 1, 1),
                1000.0,
                800.0
        );

        large.configureSeating(new SeatingPack(300, "3-3-3"));

        Aircraft a5 = new Aircraft("N789DL", b787,
                LocalDate.of(2019, 7, 30),
                6000.0,
                4500.0);

        a5.configureSeating(new SeatingPack(296, "3-3-3"));
        a5.addFeature("Lie-flat Business Class");
        a5.addFeature("WiFi");
        a5.addCertification(new AircraftCertification(
                "FAA Long-haul Certification",
                "OPERATIONAL",
                LocalDate.of(2020, 5, 1),
                LocalDate.of(2025, 5, 1)
        ));
        a5.installComponent(new InstalledComponent(
                "GEnx-1B-001",
                "GEnx-1B",
                "ACTIVE",
                a5
        ));

        AircraftModel e195 = aircraftModelRepository
                .findByModelName("E195-E2")
                .orElseThrow();

        Aircraft small = new Aircraft(
                "TEST-001",
                e195,
                LocalDate.of(2022, 1, 1),
                1000.0,
                800.0
        );

        Aircraft a6 = new Aircraft("PR-EMJ", e195,
                LocalDate.of(2022, 3, 11),
                3200.0,
                2100.0);

        a6.configureSeating(new SeatingPack(132, "2-2"));
        a6.addFeature("Wi-Fi");
        a6.addFeature("USB Charging");
        a6.installComponent(new InstalledComponent(
                "PW1900G-001",
                "PW1900G",
                "ACTIVE",
                a6
        ));
        a6.sendAircraftInFlight();
        aircraftRepository.saveAll(List.of(a1, a2, a3, a4, a5, a6,small,medium,large));
    }
}