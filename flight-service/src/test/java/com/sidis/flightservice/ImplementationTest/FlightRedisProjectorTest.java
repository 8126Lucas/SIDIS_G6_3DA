package com.sidis.flightservice.ImplementationTest;

import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.infrastructure.FlightReadRepository;
import com.sidis.flightservice.infrastructure.messaging.FlightRedisProjector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightRedisProjectorTest {
    @Mock
    private FlightReadRepository flightReadRepository;

    @InjectMocks
    private FlightRedisProjector flightRedisProjector;

    private FlightReadModel flight;

    @BeforeEach
    void setUp() {
        flight = mock(FlightReadModel.class);
    }

    @Test
    void shouldThrowExceptionWhenFlightVersionIsNull() {
        when(flight.version()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> flightRedisProjector.project(flight));
        verifyNoInteractions(flightReadRepository);
    }

    @Test
    void shouldSaveFlightWhenItDoesNotExist() {
        when(flight.flightId()).thenReturn(1L);
        when(flight.version()).thenReturn(1L);
        when(flightReadRepository.findById(1L)).thenReturn(Optional.empty());
        flightRedisProjector.project(flight);
        verify(flightReadRepository).save(flight);
    }

    @Test
    void shouldNotSaveFlightWhenStoredVersionIsEqual() {
        when(flight.flightId()).thenReturn(1L);
        when(flight.version()).thenReturn(1L);
        FlightReadModel existingFlight = mock(FlightReadModel.class);
        when(existingFlight.version()).thenReturn(1L);
        when(flightReadRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        flightRedisProjector.project(flight);
        verify(flightReadRepository, never()).save(any());
    }

    @Test
    void shouldNotSaveFlightWhenStoredVersionIsHigher() {
        when(flight.flightId()).thenReturn(1L);
        when(flight.version()).thenReturn(1L);
        FlightReadModel existingFlight = mock(FlightReadModel.class);
        when(existingFlight.version()).thenReturn(2L);
        when(flightReadRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        flightRedisProjector.project(flight);
        verify(flightReadRepository, never()).save(any());
    }

    @Test
    void shouldUpdateFlightWhenReceivedVersionIsHigher() {
        when(flight.flightId()).thenReturn(1L);
        when(flight.version()).thenReturn(2L);
        FlightReadModel existingFlight = mock(FlightReadModel.class);
        when(existingFlight.version()).thenReturn(1L);
        when(flightReadRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        flightRedisProjector.project(flight);
        verify(flightReadRepository).save(flight);
    }

    @Test
    void shouldSaveFlightWhenStoredVersionIsNull() {
        when(flight.flightId()).thenReturn(1L);
        when(flight.version()).thenReturn(1L);
        FlightReadModel existingFlight = mock(FlightReadModel.class);
        when(existingFlight.version()).thenReturn(null);
        when(flightReadRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        flightRedisProjector.project(flight);
        verify(flightReadRepository).save(flight);
    }
}
