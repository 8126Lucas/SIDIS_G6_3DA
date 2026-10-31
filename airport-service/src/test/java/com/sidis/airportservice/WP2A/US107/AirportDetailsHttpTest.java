package com.sidis.airportservice.WP2A.US107;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US107 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AirportDetailsHttpTest extends OperationalHttpTest {

    @Test void shouldReadAirportByIataAsAtcc() throws Exception {
        String code = createAirport();
        authenticateAs("atcc");
        var airport = get("/api/airports/" + code);
        assertEquals(code, airport.path("iataCode").asText());
        assertEquals("OPERATIONAL", airport.path("status").asText());
        assertTrue(airport.path("location").isObject());
        assertFalse(airport.path("runways").isEmpty());
    }

    @Test void shouldRejectUnknownAirport() throws Exception {
        authenticateAs("atcc");
        assertRejected(request("GET", "/api/airports/" + unusedIata(), null), "Unknown airport");
    }

}
