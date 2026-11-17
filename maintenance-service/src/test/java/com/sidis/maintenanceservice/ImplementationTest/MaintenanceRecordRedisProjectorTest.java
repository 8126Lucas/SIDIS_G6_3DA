package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceHoursRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordRedisProjector;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordSyncEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordRedisProjectorTest {
    @Mock
    private MaintenanceRecordQueryRepository maintenanceQueryRepository;

    @Mock
    private MaintenanceHoursRepository reportRedisRepository;

    @InjectMocks
    private MaintenanceRecordRedisProjector projector;

    private MaintenanceRecordSyncEvent createEvent(String aircraftRegistration, Double durationHours) {
        return new MaintenanceRecordSyncEvent(1L, aircraftRegistration, 10L,
                "Engine maintenance", List.of("technician1", "technician2"),
                "supervisor1", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11),
                List.of(), durationHours, new DoneListRedis("Maintenance completed", Map.of("Engine checked", true)), MaintenanceStatus.INLINE);
    }

    @Test
    void shouldUpdateExistingMaintenanceRecordInRedis() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 5.0);
        MaintenanceRecordRedis existingRecord = mock(MaintenanceRecordRedis.class);
        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.of(existingRecord));
        projector.syncAircraftToRedis(event);

        verify(maintenanceQueryRepository).findById(1L);

        verify(existingRecord).setAircraftRegistration("CS-TST");
        verify(existingRecord).setStatus(MaintenanceStatus.INLINE);
        verify(existingRecord).setMaintenanceTemplateId(10L);
        verify(existingRecord).setTechniciansUsername(List.of("technician1", "technician2"));
        verify(existingRecord).setSupervisorUsername("supervisor1");
        verify(existingRecord).setDescription("Engine maintenance");
        verify(existingRecord).setStartDate(LocalDate.of(2026, 1, 10));
        verify(existingRecord).setEndDate(LocalDate.of(2026, 1, 11));
        verify(existingRecord).setUsedParts(anyList());
        verify(existingRecord).setDurationHours(5.0);
        verify(existingRecord).setDoneList(any(DoneListRedis.class));

        verify(maintenanceQueryRepository).save(existingRecord);
    }

    @Test
    void shouldCreateMaintenanceRecordWhenItDoesNotExistInRedis() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 5.0);
        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.empty());
        projector.syncAircraftToRedis(event);
        ArgumentCaptor<MaintenanceRecordRedis> captor = ArgumentCaptor.forClass(MaintenanceRecordRedis.class);
        verify(maintenanceQueryRepository).save(captor.capture());

        MaintenanceRecordRedis savedRecord = captor.getValue();

        assertNotNull(savedRecord);
        assertEquals("CS-TST", savedRecord.getAircraftRegistration());
        assertEquals(MaintenanceStatus.INLINE, savedRecord.getStatus());
        assertEquals(10L, savedRecord.getMaintenanceTemplateId());
        assertEquals("Engine maintenance", savedRecord.getDescription());
        assertEquals(5.0, savedRecord.getDurationHours());
        assertEquals("supervisor1", savedRecord.getSupervisorUsername());
        assertEquals(List.of("technician1", "technician2"), savedRecord.getTechniciansUsername());
        assertEquals(LocalDate.of(2026, 1, 10), savedRecord.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 11), savedRecord.getEndDate());
        assertEquals(event.doneList().getCompletionNotes(), savedRecord.getDoneList().getCompletionNotes());
    }

    @Test
    void shouldUpdateMaintenanceHoursReportWhenDurationChanges() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 8.0);
        MaintenanceRecordRedis existingRecord = mock(MaintenanceRecordRedis.class);
        MaintenanceHoursReportRedis report = mock(MaintenanceHoursReportRedis.class);
        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.of(existingRecord));
        when(existingRecord.getDurationHours()).thenReturn(5.0);
        when(reportRedisRepository.findById("CS-TST")).thenReturn(Optional.of(report));
        when(report.getTotalMaintenanceHours()).thenReturn(20.0);
        projector.syncAircraftToRedis(event);
        verify(report).setTotalMaintenanceHours(23.0);
        verify(reportRedisRepository).save(report);
    }

    @Test
    void shouldCreateMaintenanceHoursReportWhenItDoesNotExist() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 5.0);
        MaintenanceRecordRedis existingRecord = mock(MaintenanceRecordRedis.class);

        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.of(existingRecord));
        when(existingRecord.getDurationHours()).thenReturn(0.0);
        when(reportRedisRepository.findById("CS-TST")).thenReturn(Optional.empty());

        projector.syncAircraftToRedis(event);
        ArgumentCaptor<MaintenanceHoursReportRedis> captor = ArgumentCaptor.forClass(MaintenanceHoursReportRedis.class);

        verify(reportRedisRepository).save(captor.capture());
        assertEquals(5.0, captor.getValue().getTotalMaintenanceHours());
    }

    @Test
    void shouldNotUpdateReportWhenDurationDoesNotChange() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 5.0);
        MaintenanceRecordRedis existingRecord = mock(MaintenanceRecordRedis.class);

        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.of(existingRecord));
        when(existingRecord.getDurationHours()).thenReturn(5.0);
        projector.syncAircraftToRedis(event);

        verifyNoInteractions(reportRedisRepository);
        verify(maintenanceQueryRepository).save(existingRecord);
    }

    @Test
    void shouldNotUpdateReportWhenAircraftRegistrationIsNull() {
        MaintenanceRecordSyncEvent event = createEvent(null, 5.0);
        when(maintenanceQueryRepository.findById(1L)).thenReturn(Optional.empty());
        projector.syncAircraftToRedis(event);
        verifyNoInteractions(reportRedisRepository);
        verify(maintenanceQueryRepository).save(any(MaintenanceRecordRedis.class));
    }

    @Test
    void shouldNotSaveRecordWhenRepositoryThrowsException() {
        MaintenanceRecordSyncEvent event = createEvent("CS-TST", 5.0);
        when(maintenanceQueryRepository.findById(1L)).thenThrow(new RuntimeException("Redis connection error"));
        assertThrows(RuntimeException.class, () -> projector.syncAircraftToRedis(event));
        verify(maintenanceQueryRepository, never()).save(any(MaintenanceRecordRedis.class));
    }
}
