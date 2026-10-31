package com.sidis.airportservice.WP2B.US208;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US208 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class UpdateAirportDetailsHttpTest extends OperationalHttpTest {

    @Test void shouldPersistNameAndContactDetails() throws Exception {
        String code = createAirport();
        String name = unique();
        var contacts = List.of(Map.of("type", "PHONE", "value", "+351210000000"));
        expect(200, request("PATCH", "/api/airports/" + code + "/details",
                Map.of("name", name, "operationalHours", "06:00-23:00", "contacts", contacts)));
        var airport = get("/api/airports/" + code);
        assertEquals(name, airport.path("name").asText());
        assertEquals(JSON.valueToTree(contacts), airport.path("contacts"));
    }

    @Test void shouldPersistOperationalHours() throws Exception {
        String code = createAirport();
        expect(200, request("PATCH", "/api/airports/" + code + "/details",
                Map.of("name", unique(), "operationalHours", "06:00-23:00", "contacts", List.of())));
        // US208 requires the hours to be retained, not just accepted in a PATCH body.
        assertEquals("06:00-23:00", get("/api/airports/" + code).path("operationalHours").asText());
    }

}
