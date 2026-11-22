package com.sidis.airportservice.ImplementationTest;

import com.sidis.airportservice.Airport.domain.AirportReadModel;
import com.sidis.airportservice.Airport.infrastructure.AirportReadRepository;
import com.sidis.airportservice.infrastructure.messaging.AirportRedisProjector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirportRedisProjectorTest {
    @Mock
    private AirportReadRepository airportReadRepository;

    @InjectMocks
    private AirportRedisProjector airportRedisProjector;

    private AirportReadModel airport;

    @BeforeEach
    void setUp() {
        airport = mock(AirportReadModel.class);
    }

    @Test
    void shouldThrowExceptionWhenAirportVersionIsNull() {
        when(airport.version()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> airportRedisProjector.project(airport));
        verifyNoInteractions(airportReadRepository);
    }

    @Test
    void shouldSaveAirportWhenItDoesNotExist() {
        when(airport.iataCode()).thenReturn("LIS");
        when(airport.version()).thenReturn(1L);
        when(airportReadRepository.findById("LIS")).thenReturn(Optional.empty());
        airportRedisProjector.project(airport);
        verify(airportReadRepository).findById("LIS");
        verify(airportReadRepository).save(airport);
    }

    @Test
    void shouldNotSaveAirportWhenStoredVersionIsEqual() {
        when(airport.iataCode()).thenReturn("LIS");
        when(airport.version()).thenReturn(1L);
        AirportReadModel existingAirport = mock(AirportReadModel.class);
        when(existingAirport.version()).thenReturn(1L);
        when(airportReadRepository.findById("LIS")).thenReturn(Optional.of(existingAirport));
        airportRedisProjector.project(airport);
        verify(airportReadRepository).findById("LIS");
        verify(airportReadRepository, never()).save(any());
    }

    @Test
    void shouldNotSaveAirportWhenStoredVersionIsHigher() {
        when(airport.iataCode()).thenReturn("LIS");
        when(airport.version()).thenReturn(1L);
        AirportReadModel existingAirport = mock(AirportReadModel.class);
        when(existingAirport.version()).thenReturn(2L);
        when(airportReadRepository.findById("LIS")).thenReturn(Optional.of(existingAirport));
        airportRedisProjector.project(airport);
        verify(airportReadRepository).findById("LIS");
        verify(airportReadRepository, never()).save(any());
    }

    @Test
    void shouldUpdateAirportWhenReceivedVersionIsHigher() {
        when(airport.iataCode()).thenReturn("LIS");
        when(airport.version()).thenReturn(2L);
        AirportReadModel existingAirport = mock(AirportReadModel.class);
        when(existingAirport.version()).thenReturn(1L);
        when(airportReadRepository.findById("LIS")).thenReturn(Optional.of(existingAirport));
        airportRedisProjector.project(airport);
        verify(airportReadRepository).findById("LIS");
        verify(airportReadRepository).save(airport);
    }

    @Test
    void shouldSaveAirportWhenStoredVersionIsNull() {
        when(airport.iataCode()).thenReturn("LIS");
        when(airport.version()).thenReturn(1L);
        AirportReadModel existingAirport = mock(AirportReadModel.class);
        when(existingAirport.version()).thenReturn(null);
        when(airportReadRepository.findById("LIS")).thenReturn(Optional.of(existingAirport));
        airportRedisProjector.project(airport);
        verify(airportReadRepository).findById("LIS");
        verify(airportReadRepository).save(airport);
    }
}
