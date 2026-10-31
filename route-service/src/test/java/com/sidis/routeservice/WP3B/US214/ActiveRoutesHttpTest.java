package com.sidis.routeservice.WP3B.US214;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US214 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class ActiveRoutesHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"popularity", "distance"})
    void shouldReturnOnlyActiveRoutesInRequestedOrder(String sort) throws Exception {
        var active = createRoute();
        long inactive = createRoute().path("routeId").asLong();
        expect(200, request("PATCH", "/api/routes/" + inactive, Map.of("status", "INACTIVE")));
        var rows = get("/api/routes/active?sortBy=" + sort);
        find(rows, "routeId", active.path("routeId").asText());
        rows.forEach(r -> {
            assertEquals("ACTIVE", r.path("status").asText());
            assertNotEquals(inactive, r.path("routeId").asLong());
        });
        for (int i = 1; i < rows.size(); i++) {
            double previous = metric(rows.get(i - 1), sort), current = metric(rows.get(i), sort);
            assertTrue(sort.equals("distance") ? previous <= current : previous >= current,
                    "Incorrect " + sort + " order");
        }
    }

    private double metric(com.fasterxml.jackson.databind.JsonNode row, String sort) {
        return sort.equals("distance") ? row.path("distanceKm").asDouble()
                : row.path("routeHistory").path("routeUsage").asDouble();
    }

}
