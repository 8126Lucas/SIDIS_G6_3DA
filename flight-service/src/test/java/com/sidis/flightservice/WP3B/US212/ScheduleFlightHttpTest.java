package com.sidis.flightservice.WP3B.US212;

import com.sidis.flightservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US212 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class ScheduleFlightHttpTest extends OperationalHttpTest {

    @Test void shouldScheduleCompatibleAircraftAndPersistFlight() throws Exception {
        long routeId = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        LocalDateTime departure = departure();
        var flight = schedule(routeId, registration, departure);
        assertTrue(flight.path("flightId").asLong() > 0);
        assertEquals(routeId, flight.path("routeId").asLong());
        assertEquals(registration, flight.path("aircraftRegistrationNumber").asText());
        assertEquals("SCHEDULED", flight.path("status").asText());
        assertEquals(departure, LocalDateTime.parse(flight.path("scheduledDeparture").asText()));
        assertEquals(departure.plusHours(2), LocalDateTime.parse(flight.path("scheduledArrival").asText()));
        assertEquals(flight, find(get("/api/flights/aircraft/" + registration),
                "flightId", flight.path("flightId").asText()));
    }

    @Test void shouldRejectArrivalBeforeDeparture() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        LocalDateTime departure = departure();
        var body = flightBody(id, registration, departure);
        body.put("scheduledArrival", departure.minusHours(1).toString());
        assertRejectedFor(request("POST", "/api/flights/scheduled", body), "arrival");
        assertTrue(get("/api/flights/aircraft/" + registration).isEmpty());
    }

    @Test void shouldRejectMissingDeparture() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        var body = flightBody(id, registration, departure());
        body.remove("scheduledDeparture");
        expect(400, request("POST", "/api/flights/scheduled", body));
    }

    @ParameterizedTest @ValueSource(strings = {"MAINTENANCE", "INACTIVE"})
    void shouldRejectUnavailableAircraft(String status) throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        expect(200, request("PATCH", "/api/hangar/aircraft/" + registration,
                Map.of("registrationNumber", registration, "availability", status)));
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(id, registration, departure())), "not available");
        assertTrue(get("/api/flights/aircraft/" + registration).isEmpty());
    }

    @Test void shouldRejectInactiveRoute() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        expect(200, request("PATCH", "/api/routes/" + id, Map.of("status", "INACTIVE")));
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(id, registration, departure())), "not active");
    }

    @ParameterizedTest @ValueSource(strings = {"originIataCode", "destinationIataCode"})
    void shouldRejectNonOperationalAirport(String endpoint) throws Exception {
        var route = createRoute();
        String registration = createAircraft();
        authenticateAs("backoffice");
        expect(200, request("PATCH", "/api/airports/" + route.path(endpoint).asText() + "/status",
                Map.of("status", "CLOSED")));
        authenticateAs("atcc");
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(route.path("routeId").asLong(), registration, departure())), "not operational");
    }

    @Test void shouldRejectAircraftWithInsufficientRange() throws Exception {
        var body = routeBody(createAirport(), createAirport());
        body.put("requirements", Map.of("requiredRange", 7000.0, "requiredCapacity", 100));
        authenticateAs("atcc");
        long id = expect(201, request("POST", "/api/routes", body)).path("routeId").asLong();
        String registration = createAircraft(); // Maximum range: 6150 km.
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(id, registration, departure())), "range");
    }

    @Test void shouldRejectAircraftWithInsufficientCapacity() throws Exception {
        // The assignment's operational constraints require range AND capacity compatibility.
        var body = routeBody(createAirport(), createAirport());
        body.put("requirements", Map.of("requiredRange", 1000.0, "requiredCapacity", 500));
        authenticateAs("atcc");
        long id = expect(201, request("POST", "/api/routes", body)).path("routeId").asLong();
        String registration = createAircraft(); // Standard capacity: 180.
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(id, registration, departure())), "capacity");
        assertTrue(get("/api/flights/aircraft/" + registration).isEmpty());
    }

    @Test void shouldRejectOverlappingFlightForSameAircraft() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        LocalDateTime departure = departure();
        schedule(id, registration, departure);
        assertRejectedFor(request("POST", "/api/flights/scheduled",
                flightBody(id, registration, departure.plusHours(1))), "already scheduled");
        assertEquals(1, get("/api/flights/aircraft/" + registration).size());
    }

    @Test void shouldAllowNonOverlappingFlightsForSameAircraft() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String registration = createAircraft();
        LocalDateTime departure = departure();
        schedule(id, registration, departure);
        schedule(id, registration, departure.plusDays(1));
        assertEquals(2, get("/api/flights/aircraft/" + registration).size());
    }

    @Test void shouldRejectUnknownAircraft() throws Exception {
        long id = createRoute().path("routeId").asLong();
        assertRejected(request("POST", "/api/flights/scheduled",
                flightBody(id, unique(), departure())), "Unknown aircraft");
    }

    @Test void shouldRejectUnknownRoute() throws Exception {
        String registration = createAircraft();
        assertRejected(request("POST", "/api/flights/scheduled",
                flightBody(Long.MAX_VALUE, registration, departure())), "Unknown route");
    }

}
