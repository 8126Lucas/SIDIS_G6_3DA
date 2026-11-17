package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelQueryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftModelQueryRepositoryTest {
    @Mock
    private AircraftModelQueryRepository repository;
    @Test
    void shouldFindAircraftModelByModelName() {
        AircraftModelRedis model = mock(AircraftModelRedis.class);
        when(repository.findByModelName("A320")).thenReturn(Optional.of(model));

        Optional<AircraftModelRedis> result = repository.findByModelName("A320");

        assertTrue(result.isPresent());
        assertSame(model, result.get());
        verify(repository).findByModelName("A320");
    }

    @Test
    void shouldReturnEmptyWhenModelDoesNotExist() {
        when(repository.findByModelName("UN")).thenReturn(Optional.empty());
        Optional<AircraftModelRedis> result = repository.findByModelName("UN");
        assertTrue(result.isEmpty());
        verify(repository).findByModelName("UN");
    }

    @Test
    void shouldSaveAircraftModel() {
        AircraftModelRedis model = mock(AircraftModelRedis.class);
        when(repository.save(model)).thenReturn(model);

        AircraftModelRedis savedModel = repository.save(model);

        assertNotNull(savedModel);
        assertSame(model, savedModel);
        verify(repository).save(model);
    }

    @Test
    void shouldDeleteAircraftModel() {
        AircraftModelRedis model = mock(AircraftModelRedis.class);
        repository.delete(model);
        verify(repository).delete(model);
    }

    @Test
    void shouldCheckIfAircraftModelExistsById() {
        when(repository.existsById("A320")).thenReturn(true);
        boolean exists = repository.existsById("A320");
        assertTrue(exists);
        verify(repository).existsById("A320");
    }
}