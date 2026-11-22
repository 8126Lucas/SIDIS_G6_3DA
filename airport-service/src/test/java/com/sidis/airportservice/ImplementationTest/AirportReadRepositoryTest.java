package com.sidis.airportservice.ImplementationTest;

import com.sidis.airportservice.Airport.domain.AirportReadModel;
import com.sidis.airportservice.Airport.domain.AirportStatus;
import com.sidis.airportservice.Airport.infrastructure.AirportReadRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirportReadRepositoryTest {
    @Mock
    private AirportReadRepository repository;

    @Test
    void shouldFindAirportById() {
        AirportReadModel airport = mock(AirportReadModel.class);
        when(repository.findById("LIS")).thenReturn(Optional.of(airport));
        Optional<AirportReadModel> result = repository.findById("LIS");
        assertTrue(result.isPresent());
        assertSame(airport, result.get());
        verify(repository).findById("LIS");
    }

    @Test
    void shouldReturnEmptyWhenAirportDoesNotExist() {
        when(repository.findById("XXX")).thenReturn(Optional.empty());
        Optional<AirportReadModel> result = repository.findById("XXX");
        assertTrue(result.isEmpty());
        verify(repository).findById("XXX");
    }

    @Test
    void shouldFindAirportsByStatus() {
        AirportReadModel airport1 = mock(AirportReadModel.class);
        AirportReadModel airport2 = mock(AirportReadModel.class);
        List<AirportReadModel> airports = List.of(airport1, airport2);
        when(repository.findByStatus(AirportStatus.OPERATIONAL)).thenReturn(airports);
        List<AirportReadModel> result = repository.findByStatus(AirportStatus.OPERATIONAL);
        assertEquals(2, result.size());
        assertSame(airport1, result.get(0));
        assertSame(airport2, result.get(1));
        verify(repository).findByStatus(AirportStatus.OPERATIONAL);
    }

    @Test
    void shouldReturnEmptyWhenNoAirportsHaveStatus() {
        when(repository.findByStatus(AirportStatus.OPERATIONAL)).thenReturn(List.of());
        List<AirportReadModel> result = repository.findByStatus(AirportStatus.OPERATIONAL);
        assertTrue(result.isEmpty());
        verify(repository).findByStatus(AirportStatus.OPERATIONAL);
    }

    @Test
    void shouldFindAirportsByCountry() {
        AirportReadModel airport = mock(AirportReadModel.class);
        when(repository.findByLocation_Country("Portugal")).thenReturn(List.of(airport));
        List<AirportReadModel> result = repository.findByLocation_Country("Portugal");
        assertEquals(1, result.size());
        assertSame(airport, result.get(0));
        verify(repository).findByLocation_Country("Portugal");
    }

    @Test
    void shouldReturnEmptyWhenCountryDoesNotExist() {
        when(repository.findByLocation_Country("Unknown")).thenReturn(List.of());
        List<AirportReadModel> result = repository.findByLocation_Country("Unknown");
        assertTrue(result.isEmpty());
        verify(repository).findByLocation_Country("Unknown");
    }

    @Test
    void shouldFindAirportsByCity() {
        AirportReadModel airport = mock(AirportReadModel.class);
        when(repository.findByLocation_City("Lisboa")).thenReturn(List.of(airport));
        List<AirportReadModel> result = repository.findByLocation_City("Lisboa");
        assertEquals(1, result.size());
        assertSame(airport, result.get(0));
        verify(repository).findByLocation_City("Lisboa");
    }

    @Test
    void shouldReturnEmptyWhenCityDoesNotExist() {
        when(repository.findByLocation_City("Unknown")).thenReturn(List.of());
        List<AirportReadModel> result = repository.findByLocation_City("Unknown");
        assertTrue(result.isEmpty());
        verify(repository).findByLocation_City("Unknown");
    }

    @Test
    void shouldSaveAirport() {
        AirportReadModel airport = mock(AirportReadModel.class);
        when(repository.save(airport)).thenReturn(airport);
        AirportReadModel savedAirport = repository.save(airport);
        assertNotNull(savedAirport);
        assertSame(airport, savedAirport);
        verify(repository).save(airport);
    }

    @Test
    void shouldDeleteAirport() {
        AirportReadModel airport = mock(AirportReadModel.class);
        repository.delete(airport);
        verify(repository).delete(airport);
    }

    @Test
    void shouldCheckIfAirportExistsById() {
        when(repository.existsById("LIS")).thenReturn(true);
        boolean exists = repository.existsById("LIS");
        assertTrue(exists);
        verify(repository).existsById("LIS");
    }

    @Test
    void shouldReturnFalseWhenAirportDoesNotExistById() {
        when(repository.existsById("XXX")).thenReturn(false);
        boolean exists = repository.existsById("XXX");
        assertFalse(exists);
        verify(repository).existsById("XXX");
    }
}
