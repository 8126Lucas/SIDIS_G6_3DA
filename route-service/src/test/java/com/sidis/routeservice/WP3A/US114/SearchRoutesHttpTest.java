package com.sidis.routeservice.WP3A.US114;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US114 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class SearchRoutesHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"origin", "destination", "both"})
    void shouldFilterByOriginDestinationOrBoth(String filter) throws Exception {
        var target = createRoute();
        createRoute(); // Unrelated data must not leak into the result.
        String origin = target.path("originIataCode").asText();
        String destination = target.path("destinationIataCode").asText();
        String query = switch (filter) {
            case "origin" -> "origin=" + origin;
            case "destination" -> "destination=" + destination;
            default -> "origin=" + origin + "&destination=" + destination;
        };
        var rows = get("/api/routes?" + query);
        assertEquals(1, rows.size());
        assertEquals(target.path("routeId"), rows.get(0).path("routeId"));
    }

    @Test void shouldReturnEmptyWhenNoRouteMatches() throws Exception {
        String code = createAirport();
        authenticateAs("atcc");
        assertTrue(get("/api/routes?origin=" + code).isEmpty());
    }

}
