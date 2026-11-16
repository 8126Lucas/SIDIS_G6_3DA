package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordQueryRepositoryTest {
    @Mock
    private MaintenanceRecordQueryRepository repository;

    private MaintenanceRecordRedis createRecord() {
        return new MaintenanceRecordRedis("CS-TST", 10L, 5.0,
                "Engine maintenance", LocalDate.of(2026, 1, 10));
    }

    @Test
    void shouldFindRecordById() {
        MaintenanceRecordRedis record = createRecord();
        when(repository.findById(1L)).thenReturn(Optional.of(record));
        Optional<MaintenanceRecordRedis> result = repository.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("CS-TST", result.get().getAircraftRegistration());
        verify(repository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenRecordDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        Optional<MaintenanceRecordRedis> result = repository.findById(999L);
        assertTrue(result.isEmpty());
        verify(repository).findById(999L);
    }

    @Test
    void shouldFindRecordByRecordId() {
        MaintenanceRecordRedis record = createRecord();
        when(repository.findByRecordId(1L)).thenReturn(Optional.of(record));
        Optional<MaintenanceRecordRedis> result = repository.findByRecordId(1L);
        assertTrue(result.isPresent());
        assertEquals("CS-TST", result.get().getAircraftRegistration());
        verify(repository).findByRecordId(1L);
    }

    @Test
    void shouldFindRecordsByStatus() {
        MaintenanceRecordRedis first = createRecord();
        MaintenanceRecordRedis second = createRecord();
        when(repository.findByStatus(MaintenanceStatus.INLINE)).thenReturn(List.of(first, second));
        List<MaintenanceRecordRedis> result = repository.findByStatus(MaintenanceStatus.INLINE);
        assertEquals(2, result.size());
        verify(repository).findByStatus(MaintenanceStatus.INLINE);
    }

    @Test
    void shouldSaveRecord() {
        MaintenanceRecordRedis record = createRecord();
        when(repository.save(record)).thenReturn(record);
        MaintenanceRecordRedis savedRecord = repository.save(record);
        assertNotNull(savedRecord);
        assertEquals("CS-TST", savedRecord.getAircraftRegistration());
        verify(repository).save(record);
    }

    @Test
    void shouldDeleteRecord() {
        MaintenanceRecordRedis record = createRecord();
        repository.delete(record);
        verify(repository).delete(record);
    }

    @Test
    void shouldFindAllRecords() {
        MaintenanceRecordRedis first = createRecord();
        MaintenanceRecordRedis second = createRecord();
        when(repository.findAll()).thenReturn(List.of(first, second));
        List<MaintenanceRecordRedis> result = repository.findAll();
        assertEquals(2, result.size());
        verify(repository).findAll();
    }
}

