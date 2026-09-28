package com.sidis.maintenanceservice.WP4B.US219;

import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OngoingMaintenanceUseCaseTest {

    private MaintenanceRecordRepository maintenanceRecordRepository;
    private MaintenanceReportService service;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        service = new MaintenanceReportService(maintenanceRecordRepository);
    }

    @Test
    void shouldReturnOngoingMaintenanceActivities() {
        MaintenanceRecord record = mock(MaintenanceRecord.class);
        when(maintenanceRecordRepository.findByStatus(MaintenanceStatus.ONGOING))
                .thenReturn(List.of(record));

        List<MaintenanceRecord> result = service.ongoingMaintenanceActivities();

        assertEquals(1, result.size());
        assertEquals(record, result.get(0));
        verify(maintenanceRecordRepository, times(1)).findByStatus(MaintenanceStatus.ONGOING);
    }
}
