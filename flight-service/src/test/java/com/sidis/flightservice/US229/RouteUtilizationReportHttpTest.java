package com.sidis.flightservice.US229;

import com.sidis.flightservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US229 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class RouteUtilizationReportHttpTest extends OperationalHttpTest {

    @Test void shouldCountFlightsPerRouteAndRankMostFrequentlyFlownFirst() throws Exception {
        var busiest = createRoute();
        var lessUsed = createRoute();
        String registration = createAircraft();
        LocalDateTime departure = departure();
        schedule(busiest.path("routeId").asLong(), registration, departure);
        schedule(busiest.path("routeId").asLong(), registration, departure.plusDays(1));
        schedule(lessUsed.path("routeId").asLong(), registration, departure.plusDays(2));
        authenticateAs("backoffice");
        var rows = get("/api/flights/reports/route-utilization");
        var first = find(rows, "routeId", busiest.path("routeId").asText());
        var second = find(rows, "routeId", lessUsed.path("routeId").asText());
        assertEquals(2, first.path("flightCount").asLong());
        assertEquals(1, second.path("flightCount").asLong());
        assertEquals(busiest.path("routeName"), first.path("routeName"));
        assertEquals(busiest.path("originIataCode").asText(), first.path("originIata").asText());
        assertEquals(busiest.path("destinationIataCode").asText(), first.path("destinationIata").asText());
        Set<Long> ids = new HashSet<>();
        long previous = Long.MAX_VALUE;
        for (var row : rows) {
            assertTrue(ids.add(row.path("routeId").asLong()), "Each route must have one aggregated row");
            assertTrue(row.path("flightCount").asLong() <= previous, "Most used routes must appear first");
            previous = row.path("flightCount").asLong();
        }
    }

}
