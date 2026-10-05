package com.sidis.maintenanceservice.WP4B.US220;

import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MaintenanceCostReportUseCaseTest {

    private MaintenanceRecordRepository maintenanceRecordRepository;
    private AircraftRestClientService aircraftClient;
    private MaintenanceReportService service;
    private MaintenanceTemplate template;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        aircraftClient = mock(AircraftRestClientService.class);
        service = new MaintenanceReportService(aircraftClient, maintenanceRecordRepository);
        template = new MaintenanceTemplate("Engine Check", 2.0,
                Map.of(), MaintenanceType.INSPECTION, MaintenanceAttribute.ENGINE);
    }

    @Test
    void shouldCalculateCostsByAircraft() {
        MaintenanceRecord record = record("CS-TVA",
                List.of(new UsedPart("P1", 2, 50.0), new UsedPart("P2", 1, 25.0)));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<MaintenanceReportService.MaintenanceCostReport> result =
                service.maintenanceCosts("aircraft");

        assertEquals(1, result.size());
        assertEquals("CS-TVA", result.get(0).group());
        assertEquals(125.0, result.get(0).totalCost());
    }

    /*@Test
    void shouldCalculateCostsByAircraftModel() {
        MaintenanceRecord first = record("CS-TVA",
                List.of(new UsedPart("P1", 1, 100.0)));
        MaintenanceRecord second = record("CS-TVB",
                List.of(new UsedPart("P2", 3, 50.0)));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(first, second));

        List<MaintenanceReportService.MaintenanceCostReport> result =
                service.maintenanceCosts("model");

        assertEquals(1, result.size());
        assertEquals("Boeing 737", result.get(0).group());
        assertEquals(250.0, result.get(0).totalCost());
    }*/

    private MaintenanceRecord record(String aircraftRegistration, List<UsedPart> parts) {
        return new MaintenanceRecord(parts, 4.0, LocalDate.of(2026, 1, 1),
                null, "maintenance", List.of(), template, aircraftRegistration);
    }
}
