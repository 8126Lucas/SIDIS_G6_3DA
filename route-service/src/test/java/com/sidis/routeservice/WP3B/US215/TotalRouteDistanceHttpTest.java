package com.sidis.routeservice.WP3B.US215;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US215 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class TotalRouteDistanceHttpTest extends OperationalHttpTest {

    @Test void shouldSumDistancesAndIncludeANewRouteExactlyOnce() throws Exception {
        authenticateAs("atcc");
        double before = get("/api/routes/total-distance").path("totalDistanceKm").asDouble();
        var route = createRoute();
        double after = get("/api/routes/total-distance").path("totalDistanceKm").asDouble();
        double expected = 0;
        for (var row : get("/api/routes")) expected += row.path("distanceKm").asDouble();
        assertTrue(route.path("distanceKm").asDouble() > 0);
        assertEquals(before + route.path("distanceKm").asDouble(), after, 0.001);
        assertEquals(expected, after, 0.001);
    }

}
