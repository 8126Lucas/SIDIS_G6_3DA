package com.sidis.airportservice.WP2A.US106a;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US106a acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AirportCertificationHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"backoffice", "atcc"})
    void shouldPersistAircraftModelCertification() throws Exception {
        String code = createAirport();
        String registration = createAircraft();

        authenticateAs("backoffice");
        String model = get("/api/hangar/aircraft/" + registration).path("modelName").asText();
        var body = Map.of("name", model, "category", "AIRCRAFT_MODEL",
                "startsAt", LocalDate.now().toString(), "expiresAt", LocalDate.now().plusYears(1).toString());

        authenticateAs("backoffice");
        expect(201, request("POST", "/api/airports/" + code + "/certifications", body));

        var certification = find(get("/api/airports/" + code).path("certifications"), "name", model);
        assertEquals("AIRCRAFT_MODEL", certification.path("category").asText());
        assertEquals(body.get("startsAt"), certification.path("startsAt").asText());
        assertEquals(body.get("expiresAt"), certification.path("expiresAt").asText());
    }

    @Test void shouldRejectCertificationForMissingAirport() throws Exception {
        String code = unusedIata();
        authenticateAs("backoffice");
        assertRejected(request("POST", "/api/airports/" + code + "/certifications",
                Map.of("name", "A320", "category", "AIRCRAFT_MODEL",
                        "startsAt", "2026-01-01", "expiresAt", "2030-01-01")), "Missing airport");
    }

    @Test void shouldRejectCertificationWithExpiryBeforeStart() throws Exception {
        String code = createAirport();
        authenticateAs("backoffice");
        assertRejected(request("POST", "/api/airports/" + code + "/certifications",
                Map.of("name", "A320", "category", "AIRCRAFT_MODEL",
                        "startsAt", "2030-01-01", "expiresAt", "2029-01-01")), "Invalid certification dates");
    }

}
