package com.sidis.maintenanceservice.WP4B.US222;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.Aircraft.infrastructure.AircraftRepository;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.maintenanceservice.application.MaintenanceAlertService;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceAlertUseCaseTest {

//    private AircraftRepository aircraftRepository;
    private MaintenanceRecordRepository maintenanceRecordRepository;
    private MaintenanceAlertService service;

//    private AircraftModel model;
    private MaintenanceTemplate template;

    @BeforeEach
    void setUp() {
//        aircraftRepository = mock(AircraftRepository.class);
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
//        service = new MaintenanceAlertService(aircraftRepository, maintenanceRecordRepository);

//        model = new AircraftModel("Boeing 737", "Boeing", null, List.of());
        template = new MaintenanceTemplate("Scheduled Check", 4.0, Map.of(), MaintenanceType.SCHEDULED, MaintenanceAttribute.AIRFRAME);
    }

    /*@Test
    void shouldAlertWhenAircraftHasNoCompletedMaintenance() {
        Aircraft aircraft = new Aircraft("CS-TVA", model, LocalDate.of(2018, 1, 1), 0.0, 100.0);

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of());

        List<MaintenanceAlertService.MaintenanceAlert> alerts = service.findAircraftDueForMaintenance(180, 500.0);

        assertEquals(1, alerts.size());
        assertEquals("CS-TVA", alerts.get(0).registrationNumber());
        assertTrue(alerts.get(0).alertReason().contains("No completed maintenance on record"));
    }*/

    /*@Test
    void shouldAlertWhenDaysSinceLastMaintenanceExceedsThreshold() {
        Aircraft aircraft = new Aircraft("CS-TVA", model, LocalDate.of(2018, 1, 1), 0.0, 100.0);

        MaintenanceRecord record = new MaintenanceRecord(aircraft, template, 4.0, "check", LocalDate.of(2025, 11, 1));
        record.markAsCompleted("done", Map.of(), LocalDate.of(2025, 11, 5));

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of(record));

        List<MaintenanceAlertService.MaintenanceAlert> alerts = service.findAircraftDueForMaintenance(180, 9999.0);

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).alertReason().contains("days ago"));
        assertNotNull(alerts.get(0).lastMaintenanceDate());
        assertNotNull(alerts.get(0).daysSinceLastMaintenance());
    }*/

    /*@Test
    void shouldAlertWhenFlightHoursExceedThreshold() {
        Aircraft aircraft = new Aircraft("CS-TUB", model, LocalDate.of(2018, 1, 1), 0.0, 650.0);

        MaintenanceRecord record = new MaintenanceRecord(aircraft, template, 4.0, "check", LocalDate.of(2026, 5, 1));
        record.markAsCompleted("done", Map.of(), LocalDate.of(2026, 5, 3));

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of(record));

        List<MaintenanceAlertService.MaintenanceAlert> alerts = service.findAircraftDueForMaintenance(9999, 500.0);

        assertEquals(1, alerts.size());
        assertEquals("CS-TUB", alerts.get(0).registrationNumber());
        assertTrue(alerts.get(0).alertReason().contains("Total flight hours"));
        assertEquals(650.0, alerts.get(0).totalFlightHours());
    }*/

    /*@Test
    void shouldNotAlertWhenMaintenanceIsRecentAndFlightHoursAreLow() {
        Aircraft aircraft = new Aircraft("CS-TVC", model, LocalDate.of(2020, 1, 1), 0.0, 200.0);

        MaintenanceRecord record = new MaintenanceRecord(aircraft, template, 4.0, "check", LocalDate.now().minusDays(10));
        record.markAsCompleted("done", Map.of(), LocalDate.now().minusDays(8));

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of(record));

        List<MaintenanceAlertService.MaintenanceAlert> alerts = service.findAircraftDueForMaintenance(180, 500.0);

        assertTrue(alerts.isEmpty());
    }*/
}