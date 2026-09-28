package com.sidis.maintenanceservice.WP4B.US220;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MaintenanceCostReportUseCaseTest {

    private MaintenanceRecordRepository maintenanceRecordRepository;
    private MaintenanceReportService service;
    private MaintenanceTemplate template;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        service = new MaintenanceReportService(maintenanceRecordRepository);
        template = new MaintenanceTemplate("Engine Check", 2.0,
                Map.of(), MaintenanceType.INSPECTION, MaintenanceAttribute.ENGINE);
    }

    /*@Test
    void shouldCalculateCostsByAircraft() {
        Aircraft aircraft = aircraft("CS-TVA", "Boeing 737");
        MaintenanceRecord record = record(aircraft,
                List.of(new UsedPart("P1", 2, 50.0), new UsedPart("P2", 1, 25.0)));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<MaintenanceReportService.MaintenanceCostReport> result =
                service.maintenanceCosts("aircraft");

        assertEquals(1, result.size());
        assertEquals("CS-TVA", result.get(0).group());
        assertEquals(125.0, result.get(0).totalCost());
    }*/

    /*@Test
    void shouldCalculateCostsByAircraftModel() {
        MaintenanceRecord first = record(aircraft("CS-TVA", "Boeing 737"),
                List.of(new UsedPart("P1", 1, 100.0)));
        MaintenanceRecord second = record(aircraft("CS-TVB", "Boeing 737"),
                List.of(new UsedPart("P2", 3, 50.0)));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(first, second));

        List<MaintenanceReportService.MaintenanceCostReport> result =
                service.maintenanceCosts("model");

        assertEquals(1, result.size());
        assertEquals("Boeing 737", result.get(0).group());
        assertEquals(250.0, result.get(0).totalCost());
    }*/

    /*private Aircraft aircraft(String registrationNumber, String modelName) {
        return new Aircraft(registrationNumber,
                new AircraftModel(modelName, "Manufacturer", null, List.of()),
                LocalDate.of(2018, 1, 1), 0.0, 0.0);
    }

    private MaintenanceRecord record(Aircraft aircraft, List<UsedPart> parts) {
        return new MaintenanceRecord(parts, 4.0, LocalDate.of(2026, 1, 1),
                null, "maintenance", List.of(), template, aircraft);
    }*/
}
