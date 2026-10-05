package com.sidis.maintenanceservice.WP4B.US221;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TurnaroundAverageUseCaseTest {

    private MaintenanceRecordRepository maintenanceRecordRepository;
    private AircraftRestClientService aircraftClient;
    private MaintenanceReportService service;
    private MaintenanceTemplate template;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        service = new MaintenanceReportService(aircraftClient, maintenanceRecordRepository);
        template = new MaintenanceTemplate("Engine Check", 2.0, Map.of(), MaintenanceType.INSPECTION, MaintenanceAttribute.ENGINE);
    }

    @Test
    void shouldReturnEmptyListWhenNoCompletedRecords() {
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of());

        List<MaintenanceReportService.TurnaroundAverage> result = service.averageTurnaroundTimePerAircraftType();

        assertTrue(result.isEmpty());
        verify(maintenanceRecordRepository, times(1)).findByStatus(MaintenanceStatus.COMPLETED);
    }

    /*@Test
    void shouldCalculateAverageForSingleAircraftType() {
        MaintenanceRecord r1 = new MaintenanceRecord("CS-TVA", template, 6.0, "check 1", LocalDate.of(2026, 1, 1));
        r1.markAsCompleted("done", Map.of(), LocalDate.of(2026, 1, 6)); // 5 days

        MaintenanceRecord r2 = new MaintenanceRecord("CS-TVA", template, 6.0, "check 2", LocalDate.of(2026, 2, 1));
        r2.markAsCompleted("done", Map.of(), LocalDate.of(2026, 2, 11)); // 10 days

        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of(r1, r2));

        List<MaintenanceReportService.TurnaroundAverage> result = service.averageTurnaroundTimePerAircraftType();

        assertEquals(1, result.size());
        assertEquals(7.5, result.get(0).averageDays());
    }*/

    /*@Test
    void shouldCalculateAveragePerAircraftType() {
        Aircraft ac1 = new Aircraft("CS-TVA", boeing737, LocalDate.of(2018, 1, 1), 0.0, 0.0);
        Aircraft ac2 = new Aircraft("CS-TUB", airbusA320, LocalDate.of(2020, 1, 1), 0.0, 0.0);

        MaintenanceRecord r1 = new MaintenanceRecord(ac1, template, 6.0, "check", LocalDate.of(2026, 1, 1));
        r1.markAsCompleted("done", Map.of(), LocalDate.of(2026, 1, 11)); // 10 days

        MaintenanceRecord r2 = new MaintenanceRecord(ac2, template, 4.0, "check", LocalDate.of(2026, 1, 1));
        r2.markAsCompleted("done", Map.of(), LocalDate.of(2026, 1, 4)); // 3 days

        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.COMPLETED)).thenReturn(List.of(r1, r2));

        List<MaintenanceReportService.TurnaroundAverage> result = service.averageTurnaroundTimePerAircraftType();

        assertEquals(2, result.size());

        MaintenanceReportService.TurnaroundAverage airbus = result.stream()
                .filter(t -> t.aircraftType().equals("Airbus A320")).findFirst().orElseThrow();
        MaintenanceReportService.TurnaroundAverage boeing = result.stream()
                .filter(t -> t.aircraftType().equals("Boeing 737")).findFirst().orElseThrow();

        assertEquals(3.0, airbus.averageDays());
        assertEquals(10.0, boeing.averageDays());
    }*/
}