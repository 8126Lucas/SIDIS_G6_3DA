package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelQueryRepository;

import com.sidis.aircraftservice.infrastructure.messaging.AircraftModelRedisProjector;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftModelSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftModelRedisProjectorTest {
    @Mock
    private AircraftModelQueryRepository aircraftModelQueryRepository;

    @InjectMocks
    private AircraftModelRedisProjector projector;
    @Test
    void shouldUpdateExistingAircraftModelInRedis() {
        AircraftModelRedis existingModel = mock(AircraftModelRedis.class);
        AircraftModelSyncEvent event = new AircraftModelSyncEvent("A320", List.of("image1.jpg", "image2.jpg"), "Airbus", null);

        when(aircraftModelQueryRepository.findByModelName("A320")).thenReturn(Optional.of(existingModel));
        projector.syncAircraftToRedis(event);

        verify(existingModel).changeModelName("A320");
        verify(existingModel).changeManufacturer("Airbus");
        verify(existingModel).changeAircraftModelSpecs(null);
        verify(existingModel).changeModelImage(List.of("image1.jpg", "image2.jpg"));
        verify(aircraftModelQueryRepository).save(existingModel);
    }

    @Test
    void shouldCreateAircraftModelWhenItDoesNotExistInRedis() {
        AircraftModelSyncEvent event = new AircraftModelSyncEvent("A320", List.of("image1.jpg"), "Airbus", null);

        when(aircraftModelQueryRepository.findByModelName("A320")).thenReturn(Optional.empty());
        projector.syncAircraftToRedis(event);
        ArgumentCaptor<AircraftModelRedis> captor = ArgumentCaptor.forClass(AircraftModelRedis.class);

        verify(aircraftModelQueryRepository).save(captor.capture());
        assertNotNull(captor.getValue());
        verify(aircraftModelQueryRepository).findByModelName("A320");
    }

    @Test
    void shouldNotSaveWhenRepositoryThrowsException() {
        AircraftModelSyncEvent event = new AircraftModelSyncEvent("A320", List.of("image1.jpg"), "Airbus", null);

        when(aircraftModelQueryRepository.findByModelName("A320")).thenThrow(new RuntimeException("Redis unavailable"));

        assertThrows(RuntimeException.class, () -> projector.syncAircraftToRedis(event));
        verify(aircraftModelQueryRepository, never()).save(any(AircraftModelRedis.class));
    }
}