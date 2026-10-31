package com.sidis.airportservice.WP2A.US106;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US106 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class RegisterAirportHttpTest extends OperationalHttpTest {

    @Test void shouldRegisterAirportWithLocationAndRunway() throws Exception {
        String code = unusedIata();
        var body = airportBody(code);
        expect(201, request("POST", "/api/airports", body));
        var airport = get("/api/airports/" + code);
        assertEquals(code, airport.path("iataCode").asText());
        assertEquals(body.get("name"), airport.path("name").asText());
        assertEquals(JSON.valueToTree(body.get("location")), airport.path("location"));
        assertEquals(JSON.valueToTree(body.get("runways")), airport.path("runways"));
    }

    @Test void shouldRejectDuplicateIataWithoutOverwritingAirport() throws Exception {
        String code = createAirport();
        var original = get("/api/airports/" + code);
        assertRejected(request("POST", "/api/airports", airportBody(code)), "Duplicate IATA");
        assertEquals(original, get("/api/airports/" + code));
    }

    @ParameterizedTest @ValueSource(strings = {"AB", "ABCD", "aBc", "123"})
    void shouldRejectInvalidIata(String code) throws Exception {
        assertRejected(request("POST", "/api/airports", airportBody(code)), "Invalid IATA " + code);
    }

}
