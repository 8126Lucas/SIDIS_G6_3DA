package com.sidis.maintenanceservice.US226;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.maintenanceservice.application.PartsInventoryService;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.domain.UsedPart;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartsInventoryUseCaseTest {

    private MaintenanceRecordRepository maintenanceRecordRepository;
    private PartsInventoryService service;

//    private AircraftModel model;
    private MaintenanceTemplate template;
//    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        service = new PartsInventoryService(maintenanceRecordRepository);

//        model = new AircraftModel("Boeing 737", "Boeing", null, List.of());
        template = new MaintenanceTemplate("Engine Check", 4.0, Map.of(), MaintenanceType.SCHEDULED, MaintenanceAttribute.ENGINE);
//        aircraft = new Aircraft("CS-TVA", model, LocalDate.of(2018, 1, 1), 0.0, 100.0);
    }

    @Test
    void shouldReturnEmptyInventoryWhenNoRecords() {
        when(maintenanceRecordRepository.findAll()).thenReturn(List.of());

        List<PartsInventoryService.PartUsageSummary> result = service.getPartsUsageSummary();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyInventoryWhenRecordsHaveNoParts() {
        MaintenanceRecord record = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check", LocalDate.now());
        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<PartsInventoryService.PartUsageSummary> result = service.getPartsUsageSummary();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAggregateSamePartAcrossMultipleRecords() {
        MaintenanceRecord r1 = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check A", LocalDate.now());
        r1.addUsedPart(new UsedPart("PART-001", 3, 50.0));

        MaintenanceRecord r2 = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check B", LocalDate.now());
        r2.addUsedPart(new UsedPart("PART-001", 2, 50.0));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(r1, r2));

        List<PartsInventoryService.PartUsageSummary> result = service.getPartsUsageSummary();

        assertEquals(1, result.size());
        assertEquals("PART-001", result.get(0).partSerialNumber());
        assertEquals(5, result.get(0).totalQuantityUsed());
        assertEquals(250.0, result.get(0).totalCost());
        assertEquals(2L, result.get(0).timesUsedInRecords());
    }

    @Test
    void shouldAggregateMultipleDifferentParts() {
        MaintenanceRecord record = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check", LocalDate.now());
        record.addUsedPart(new UsedPart("BOLT-XL", 10, 5.0));
        record.addUsedPart(new UsedPart("FILTER-A", 1, 120.0));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<PartsInventoryService.PartUsageSummary> result = service.getPartsUsageSummary();

        assertEquals(2, result.size());
        // results are sorted by partSerialNumber
        assertEquals("BOLT-XL", result.get(0).partSerialNumber());
        assertEquals(50.0, result.get(0).totalCost());
        assertEquals("FILTER-A", result.get(1).partSerialNumber());
        assertEquals(120.0, result.get(1).totalCost());
    }

    @Test
    void shouldReturnLowStockAlertWhenUsageExceedsThreshold() {
        MaintenanceRecord record = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check", LocalDate.now());
        record.addUsedPart(new UsedPart("PART-001", 15, 10.0));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<PartsInventoryService.LowStockAlert> alerts = service.getLowStockAlerts(10);

        assertEquals(1, alerts.size());
        assertEquals("PART-001", alerts.get(0).partSerialNumber());
        assertEquals(15, alerts.get(0).totalQuantityUsed());
        assertTrue(alerts.get(0).alertMessage().contains("threshold: 10"));
    }

    @Test
    void shouldNotAlertWhenUsageBelowThreshold() {
        MaintenanceRecord record = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check", LocalDate.now());
        record.addUsedPart(new UsedPart("PART-001", 3, 10.0));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<PartsInventoryService.LowStockAlert> alerts = service.getLowStockAlerts(10);

        assertTrue(alerts.isEmpty());
    }

    @Test
    void shouldAlertWhenUsageEqualsThreshold() {
        MaintenanceRecord record = new MaintenanceRecord(/*aircraft,*/ template, 4.0, "check", LocalDate.now());
        record.addUsedPart(new UsedPart("PART-001", 10, 10.0));

        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(record));

        List<PartsInventoryService.LowStockAlert> alerts = service.getLowStockAlerts(10);

        assertEquals(1, alerts.size());
    }
}
