package com.sidis.airportservice.WP2B.US209;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US209 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AirportRoutesHttpTest extends OperationalHttpTest {

    @Test void shouldIncludeDepartingAndArrivingRoutes() throws Exception {
        String hub = createAirport();
        var outgoing = createRoute(hub, createAirport());
        var incoming = createRoute(createAirport(), hub);
        authenticateAs("atcc");
        var rows = get("/api/airports/" + hub + "/routes");
        find(rows, "routeId", outgoing.path("routeId").asText());
        find(rows, "routeId", incoming.path("routeId").asText());
        assertEquals(2, rows.size(), "Both incoming and outgoing routes must be returned");
    }

    @Test void shouldReturnEmptyForAirportWithoutRoutes() throws Exception {
        String code = createAirport();
        authenticateAs("atcc");
        var rows = get("/api/airports/" + code + "/routes");
        assertTrue(rows.isArray());
        assertTrue(rows.isEmpty());
    }

}
