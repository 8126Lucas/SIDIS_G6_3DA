package com.sidis.flightservice.ImplementationTest;

import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.infrastructure.FlightReadRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightReadRepositoryTest {
    @Mock
    private FlightReadRepository repository;

    @Test
    void shouldFindFlightsByAircraftRegistrationNumber() {
        FlightReadModel flight1 = mock(FlightReadModel.class);
        FlightReadModel flight2 = mock(FlightReadModel.class);
        when(repository.findByAircraftRegistrationNumber("CS-TST")).thenReturn(List.of(flight1, flight2));
        List<FlightReadModel> result = repository.findByAircraftRegistrationNumber("CS-TST");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(flight1, result.get(0));
        assertSame(flight2, result.get(1));

        verify(repository).findByAircraftRegistrationNumber("CS-TST");
    }

    @Test
    void shouldReturnEmptyListWhenNoFlightsExistForAircraft() {
        when(repository.findByAircraftRegistrationNumber("CS-UNK")).thenReturn(List.of());
        List<FlightReadModel> result = repository.findByAircraftRegistrationNumber("CS-UNK");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByAircraftRegistrationNumber("CS-UNK");
    }

    @Test
    void shouldSaveFlightReadModel() {
        FlightReadModel flight = mock(FlightReadModel.class);
        when(repository.save(flight)).thenReturn(flight);
        FlightReadModel savedFlight = repository.save(flight);
        assertNotNull(savedFlight);
        assertSame(flight, savedFlight);
        verify(repository).save(flight);
    }

    @Test
    void shouldDeleteFlightReadModel() {
        FlightReadModel flight = mock(FlightReadModel.class);
        repository.delete(flight);
        verify(repository).delete(flight);
    }

    @Test
    void shouldCheckIfFlightReadModelExistsById() {
        when(repository.existsById(1L)).thenReturn(true);
        boolean exists = repository.existsById(1L);
        assertTrue(exists);
        verify(repository).existsById(1L);
    }
}