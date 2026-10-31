package com.sidis.routeservice.WP3A.US113;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US113 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class RouteDetailsHttpTest extends OperationalHttpTest {

    @Test void shouldFindRoutesFromAirportAndReadTheirDetails() throws Exception {
        String origin = createAirport();
        var first = createRoute(origin, createAirport());
        var second = createRoute(origin, createAirport());
        var rows = get("/api/routes/from/" + origin);
        assertEquals(2, rows.size());
        for (var expected : List.of(first, second)) {
            find(rows, "routeId", expected.path("routeId").asText());
            assertEquals(expected, get("/api/routes/" + expected.path("routeId").asLong()));
        }
        rows.forEach(r -> assertEquals(origin, r.path("originIataCode").asText()));
    }

    @Test void shouldRejectUnknownRoute() throws Exception {
        authenticateAs("atcc");
        assertRejected(request("GET", "/api/routes/" + Long.MAX_VALUE, null), "Unknown route");
    }

}
