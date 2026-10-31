package com.sidis.routeservice.WP3B.US216;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US216 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AlternativeRoutesHttpTest extends OperationalHttpTest {

    @Test void shouldFindActiveAlternativesBetweenSameAirports() throws Exception {
        String origin = createAirport(), destination = createAirport();
        var first = createRoute(origin, destination);
        var second = createRoute(origin, destination);
        long inactive = createRoute(origin, destination).path("routeId").asLong();
        expect(200, request("PATCH", "/api/routes/" + inactive, Map.of("status", "INACTIVE")));
        var rows = get("/api/routes/alternatives?origin=" + origin + "&destination=" + destination);
        assertEquals(2, rows.size());
        find(rows, "routeId", first.path("routeId").asText());
        find(rows, "routeId", second.path("routeId").asText());
        rows.forEach(r -> {
            assertEquals(origin, r.path("originIataCode").asText());
            assertEquals(destination, r.path("destinationIataCode").asText());
            assertEquals("ACTIVE", r.path("status").asText());
        });
    }

    @Test void shouldReturnEmptyWhenThereAreNoAlternatives() throws Exception {
        String origin = createAirport(), destination = createAirport();
        authenticateAs("atcc");
        assertTrue(get("/api/routes/alternatives?origin=" + origin + "&destination=" + destination).isEmpty());
    }

}
