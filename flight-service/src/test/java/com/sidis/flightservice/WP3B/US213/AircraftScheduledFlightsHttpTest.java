package com.sidis.flightservice.WP3B.US213;

import com.sidis.flightservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US213 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AircraftScheduledFlightsHttpTest extends OperationalHttpTest {

    @Test void shouldReturnAllFlightsOnlyForRequestedAircraft() throws Exception {
        long id = createRoute().path("routeId").asLong();
        String target = createAircraft(), other = createAircraft();
        LocalDateTime departure = departure();
        var first = schedule(id, target, departure);
        var second = schedule(id, target, departure.plusDays(1));
        schedule(id, other, departure);
        var rows = get("/api/flights/aircraft/" + target);
        assertEquals(2, rows.size());
        find(rows, "flightId", first.path("flightId").asText());
        find(rows, "flightId", second.path("flightId").asText());
        rows.forEach(f -> assertEquals(target, f.path("aircraftRegistrationNumber").asText()));
    }

    @Test void shouldReturnEmptyForAircraftWithoutScheduledFlights() throws Exception {
        String registration = createAircraft();
        var rows = get("/api/flights/aircraft/" + registration);
        assertTrue(rows.isArray());
        assertTrue(rows.isEmpty());
    }

}
