package com.sidis.flightservice.ImplementationTest;

import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.domain.FlightStatus;
import com.sidis.flightservice.Flight.domain.AircraftReference;
import com.sidis.flightservice.Flight.domain.RouteReference;
import com.sidis.flightservice.Flight.infrastructure.FlightRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("unit")
class FlightRepositoryTest {
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private EntityManager entityManager;

    private AircraftReference createAircraftReference() {
        return new AircraftReference("CS-TST");
    }

    private RouteReference createRouteReference() {
        return new RouteReference(1L, "LIS", "Lisbon - Porto", "OPO");
    }

    private Flight createFlight(LocalDateTime departure, LocalDateTime arrival) {
        return new Flight(createRouteReference(), createAircraftReference(), departure, arrival);
    }

    @Test
    void shouldSaveAndFindFlight() {
        LocalDateTime departure = LocalDateTime.of(2026, 5, 10, 10, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 5, 10, 12, 0);
        Flight flight = createFlight(departure, arrival);

        flightRepository.saveAndFlush(flight);
        Long flightId = flight.getFlightId();
        entityManager.clear();
        Optional<Flight> result = flightRepository.findByFlightId(flightId);

        assertTrue(result.isPresent());
        Flight savedFlight = result.get();

        assertEquals(flightId, savedFlight.getFlightId());
        assertEquals("CS-TST", savedFlight.getAircraft().getRegistrationNumber());
        assertEquals(1L, savedFlight.getRoute().getRouteId());
        assertEquals("LIS", savedFlight.getRoute().getOriginIata());
        assertEquals("OPO", savedFlight.getRoute().getDestinationIata());
        assertEquals(departure, savedFlight.getScheduledDeparture());
        assertEquals(arrival, savedFlight.getScheduledArrival());
        assertEquals(FlightStatus.SCHEDULED, savedFlight.getFlightStatus());
    }

    @Test
    void shouldReturnEmptyWhenFlightDoesNotExist() {
        Optional<Flight> result = flightRepository.findByFlightId(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindFlightsByAircraftRegistrationNumber() {
        Flight flight1 = createFlight(LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 10, 12, 0));
        Flight flight2 = createFlight(LocalDateTime.of(2026, 5, 11, 10, 0),
                LocalDateTime.of(2026, 5, 11, 12, 0));

        flightRepository.save(flight1);
        flightRepository.save(flight2);
        flightRepository.flush();

        entityManager.clear();

        List<Flight> result = flightRepository.findByAircraftRegistrationNumber_RegistrationNumber("CS-TST");
        assertEquals(2, result.size());

        assertTrue(result.stream().allMatch(
                flight -> flight.getAircraft()
                        .getRegistrationNumber()
                        .equals("CS-TST")
        ));
    }

    @Test
    void shouldReturnEmptyListWhenAircraftHasNoFlights() {
        List<Flight> result = flightRepository.findByAircraftRegistrationNumber_RegistrationNumber("CS-NOT");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindFlightsByAircraftAndScheduledDepartureBetween() {
        Flight flight1 = createFlight(LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 10, 12, 0));

        Flight flight2 = createFlight(LocalDateTime.of(2026, 5, 15, 10, 0),
                LocalDateTime.of(2026, 5, 15, 12, 0));

        Flight flight3 = createFlight(LocalDateTime.of(2026, 5, 20, 10, 0),
                LocalDateTime.of(2026, 5, 20, 12, 0));

        flightRepository.saveAll(List.of(flight1, flight2, flight3));
        flightRepository.flush();
        entityManager.clear();

        List<Flight> result = flightRepository
                        .findByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureBetween(
                                "CS-TST",
                                LocalDateTime.of(2026, 5, 12, 0, 0),
                                LocalDateTime.of(2026, 5, 18, 23, 59)
                        );

        assertEquals(1, result.size());
        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 0), result.get(0).getScheduledDeparture()
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoFlightsExistBetweenDates() {
        Flight flight = createFlight(LocalDateTime.of(2026, 5, 10, 10, 0), LocalDateTime.of(2026, 5, 10, 12, 0));
        flightRepository.saveAndFlush(flight);
        List<Flight> result = flightRepository
                        .findByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureBetween(
                                "CS-TST",
                                LocalDateTime.of(2026, 5, 15, 0, 0),
                                LocalDateTime.of(2026, 5, 20, 23, 59)
                        );
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenAircraftHasOverlappingFlight() {
        Flight flight = createFlight(LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 10, 12, 0));
        flightRepository.saveAndFlush(flight);
        boolean result = flightRepository
                        .existsByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
                                "CS-TST",
                                LocalDateTime.of(2026, 5, 10, 11, 0),
                                LocalDateTime.of(2026, 5, 10, 11, 0));
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenAircraftHasNoOverlappingFlight() {
        Flight flight = createFlight(LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 10, 12, 0));
        flightRepository.saveAndFlush(flight);
        boolean result = flightRepository.existsByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
                                "CS-TST",
                                LocalDateTime.of(2026, 5, 10, 15, 0),
                                LocalDateTime.of(2026, 5, 10, 14, 0));
        assertFalse(result);
    }
}