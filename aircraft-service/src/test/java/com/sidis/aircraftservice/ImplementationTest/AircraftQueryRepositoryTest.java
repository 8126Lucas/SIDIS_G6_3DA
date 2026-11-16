package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftQueryRepositoryTest {
    @Mock
    private AircraftQueryRepository repository;

    @Test
    void shouldFindAircraftById() {
        AircraftRedis aircraft = mock(AircraftRedis.class);
        when(repository.findById("CS-TST")).thenReturn(Optional.of(aircraft));
        Optional<AircraftRedis> result = repository.findById("CS-TST");
        assertTrue(result.isPresent());
        assertSame(aircraft, result.get());
        verify(repository).findById("CS-TST");
    }

    @Test
    void shouldReturnEmptyWhenAircraftDoesNotExist() {
        when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());
        Optional<AircraftRedis> result = repository.findById("UNKNOWN");
        assertTrue(result.isEmpty());
        verify(repository).findById("UNKNOWN");
    }

    @Test
    void shouldFindAircraftByRegistrationNumber() {
        AircraftRedis aircraft = mock(AircraftRedis.class);
        when(repository.findByRegistrationNumber("CS-TST")).thenReturn(Optional.of(aircraft));
        Optional<AircraftRedis> result = repository.findByRegistrationNumber("CS-TST");
        assertTrue(result.isPresent());
        assertSame(aircraft, result.get());
        verify(repository).findByRegistrationNumber("CS-TST");
    }

    @Test
    void shouldSaveAircraft() {
        AircraftRedis aircraft = mock(AircraftRedis.class);
        when(repository.save(aircraft)).thenReturn(aircraft);
        AircraftRedis savedAircraft = repository.save(aircraft);
        assertNotNull(savedAircraft);
        assertSame(aircraft, savedAircraft);
        verify(repository).save(aircraft);
    }

    @Test
    void shouldDeleteAircraft() {
        AircraftRedis aircraft = mock(AircraftRedis.class);
        repository.delete(aircraft);
        verify(repository).delete(aircraft);
    }

    @Test
    void shouldCheckIfAircraftExistsById() {
        when(repository.existsById("CS-TST")).thenReturn(true);
        boolean exists = repository.existsById("CS-TST");
        assertTrue(exists);
        verify(repository).existsById("CS-TST");
    }

    @Test
    void shouldCountAircraftByStatus() {
        when(repository.countByStatus(AircraftAvailability.AVAILABLE)).thenReturn(5L);
        long count = repository.countByStatus(AircraftAvailability.AVAILABLE);
        assertEquals(5L, count);
        verify(repository).countByStatus(AircraftAvailability.AVAILABLE);
    }
}