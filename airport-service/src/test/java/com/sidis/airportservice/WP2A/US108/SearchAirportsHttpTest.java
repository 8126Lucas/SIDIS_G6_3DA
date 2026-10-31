package com.sidis.airportservice.WP2A.US108;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US108 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class SearchAirportsHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"city", "country", "name"})
    void shouldFilterAirports(String filter) throws Exception {
        String code = createAirport();
        var airport = get("/api/airports/" + code);
        String value = filter.equals("name") ? airport.path("name").asText()
                : airport.path("location").path(filter).asText();
        authenticateAs("atcc");
        var rows = get("/api/airports?" + filter + "=" + value);
        assertEquals(1, rows.size(), "Unique fixture must be the only match");
        assertEquals(code, rows.get(0).path("iataCode").asText());
    }

    @Test void shouldReturnEmptyWhenNoAirportMatches() throws Exception {
        authenticateAs("atcc");
        assertTrue(get("/api/airports?city=" + unique()).isEmpty());
    }

}
