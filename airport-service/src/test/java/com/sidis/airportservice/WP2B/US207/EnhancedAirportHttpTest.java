package com.sidis.airportservice.WP2B.US207;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US207 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class EnhancedAirportHttpTest extends OperationalHttpTest {

    @Test void shouldPersistPhotosAndFacilities() throws Exception {
        String code = unusedIata();
        var body = airportBody(code);
        expect(201, request("POST", "/api/airports", body));
        var airport = get("/api/airports/" + code);
        assertEquals(JSON.valueToTree(body.get("airportPhotos")), airport.path("airportPhotos"));
        assertEquals(JSON.valueToTree(body.get("facilities")), airport.path("facilities"));
    }

    @Test void shouldAllowRegistrationWithoutOptionalPhotos() throws Exception {
        String code = unusedIata();
        var body = airportBody(code);
        body.remove("airportPhotos");
        expect(201, request("POST", "/api/airports", body));
        assertTrue(get("/api/airports/" + code).path("airportPhotos").isArray());
        assertTrue(get("/api/airports/" + code).path("airportPhotos").isEmpty());
    }

}
