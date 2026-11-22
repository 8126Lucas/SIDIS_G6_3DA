package com.sidis.airportservice.ImplementationTest;

import com.sidis.airportservice.Airport.domain.Airport;
import com.sidis.airportservice.Airport.domain.AirportLocation;
import com.sidis.airportservice.Airport.domain.AirportStatus;
import com.sidis.airportservice.Airport.domain.Facilities;
import com.sidis.airportservice.Airport.infrastructure.AirportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("unit")
class AirportRepositoryTest {
    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private EntityManager entityManager;

    private AirportLocation createAirportLocation() {
        return new AirportLocation("Lisbon", -9.1354, 38.7813,
                "Lisbon", "Europe/Lisbon", "Portugal");
    }

    private Facilities createFacilities() {
        return new Facilities(2, List.of("WiFi", "Parking", "Restaurants"), 40);
    }

    private Airport createAirport(String iataCode, String name) {
        return new Airport(iataCode, "International", name, AirportStatus.values()[0],
                createAirportLocation(), createFacilities(), 250.0, List.of(),
                List.of(), List.of(), List.of("https://example.com/airport.jpg"));
    }

    @Test
    void shouldSaveAndFindAirport() {
        Airport airport = createAirport("LIS", "Lisbon Airport");
        airportRepository.saveAndFlush(airport);
        entityManager.clear();
        Optional<Airport> result = airportRepository.findById("LIS");
        assertTrue(result.isPresent());
        assertEquals("LIS", result.get().getIataCode());
        assertEquals("Lisbon Airport", result.get().getName());
        assertEquals("International", result.get().getAirportType());
        assertEquals("Lisbon", result.get().getAirportLocation().getCity());
        assertEquals("Portugal", result.get().getAirportLocation().getCountry());
        assertEquals(250.0, result.get().getRouteDistance());
        assertEquals(2, result.get().getFacilities().getTerminalCount());
        assertEquals(40, result.get().getFacilities().getGateCount());
        assertEquals("07:00-22:00", result.get().getOperationalHours());
    }

    @Test
    void shouldReturnEmptyWhenAirportDoesNotExist() {
        Optional<Airport> result = airportRepository.findById("XXX");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindAirportByIataCode() {
        Airport airport = createAirport("LIS", "Lisbon Airport");
        airportRepository.saveAndFlush(airport);
        entityManager.clear();
        Optional<Airport> result = airportRepository.findByIataCode("LIS");
        assertTrue(result.isPresent());
        assertEquals("LIS", result.get().getIataCode());
        assertEquals("Lisbon Airport", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenIataCodeDoesNotExist() {
        Optional<Airport> result =airportRepository.findByIataCode("XXX");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindAirportByName() {
        Airport airport = createAirport("LIS", "Lisbon Airport");
        airportRepository.saveAndFlush(airport);
        entityManager.clear();
        Optional<Airport> result = airportRepository.findByName("Lisbon Airport");
        assertTrue(result.isPresent());
        assertEquals("LIS", result.get().getIataCode());
        assertEquals("Lisbon Airport", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenAirportNameDoesNotExist() {
        Optional<Airport> result = airportRepository.findByName("Unknown Airport");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeleteAirport() {
        Airport airport = createAirport("LIS", "Lisbon Airport");
        airportRepository.saveAndFlush(airport);
        airportRepository.delete(airport);
        airportRepository.flush();
        assertFalse(airportRepository.existsById("LIS"));
    }

    @Test
    void shouldCheckIfAirportExistsById() {
        Airport airport = createAirport("LIS", "Lisbon Airport");
        airportRepository.saveAndFlush(airport);
        assertTrue(airportRepository.existsById("LIS"));
        assertFalse(airportRepository.existsById("XXX"));
    }
}
