package com.sidis.maintenanceservice.WP4A.US116;

import com.sidis.maintenanceservice.application.AddMaintenanceRecordUseCase;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogMaintenanceActivityUseCaseToRepoTest {
    private MaintenanceRecordRepository maintenanceRecordRepository;
    private AircraftRestClientService aircraftClient;
    private AddMaintenanceRecordUseCase useCase;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository = mock(MaintenanceRecordRepository.class);
        aircraftClient = mock(AircraftRestClientService.class);
        useCase = new AddMaintenanceRecordUseCase(maintenanceRecordRepository, aircraftClient);
    }

    /*@Test
    void shouldSaveMaintenanceRecordWhenExecuteShortWithFields() {
        String aircraftRegistration = "CS-TUA";
        MaintenanceTemplate template = mock(MaintenanceTemplate.class);
        LocalDate startDate = LocalDate.of(2026, 5, 21);

        useCase.executeShort(
                aircraftRegistration,
                template,
                6.5,
                "Scheduled A-check maintenance",
                startDate
        );

        ArgumentCaptor<MaintenanceRecord> captor =
                ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository, times(1)).save(captor.capture());

        MaintenanceRecord savedRecord = captor.getValue();
        assertEquals(aircraftRegistration, savedRecord.getAircraftRegistration());
        assertEquals(template, savedRecord.getMaintenanceTemplate());
        assertEquals(6.5, savedRecord.getDurationHours());
        assertEquals("Scheduled A-check maintenance", savedRecord.getDescription());
        assertEquals(startDate, savedRecord.getStartDate());
        assertEquals(MaintenanceStatus.INLINE, savedRecord.getStatus());
    }*/

    @Test
    void shouldSaveMaintenanceRecordWhenExecuteWithRecordObject() {
        MaintenanceRecord record = mock(MaintenanceRecord.class);

        useCase.execute(record);

        verify(maintenanceRecordRepository, times(1)).save(record);
    }
}
