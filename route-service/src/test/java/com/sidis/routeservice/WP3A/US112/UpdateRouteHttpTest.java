package com.sidis.routeservice.WP3A.US112;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US112 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class UpdateRouteHttpTest extends OperationalHttpTest {

    @Test void shouldUpdateDestinationAndFlightTime() throws Exception {
        var route = createRoute();
        String destination = createAirport();
        authenticateAs("atcc");
        long id = route.path("routeId").asLong();
        expect(200, request("PATCH", "/api/routes/" + id,
                Map.of("destinationIataCode", destination, "estimatedFlightTimeHours", 3.5)));
        var saved = get("/api/routes/" + id);
        assertEquals(destination, saved.path("destinationIataCode").asText());
        assertEquals(3.5, saved.path("estimatedFlightTimeHours").asDouble());
    }

    @Test void shouldDeactivateRouteAsBackoffice() throws Exception {
        long id = createRoute().path("routeId").asLong();
        authenticateAs("backoffice");
        expect(200, request("PATCH", "/api/routes/" + id, Map.of("status", "INACTIVE")));
        assertEquals("INACTIVE", get("/api/routes/" + id).path("status").asText());
        assertTrue(java.util.stream.StreamSupport.stream(get("/api/routes/active").spliterator(), false)
                .noneMatch(r -> r.path("routeId").asLong() == id));
    }

    @Test void shouldRejectInvalidDurationWithoutChangingRoute() throws Exception {
        var route = createRoute();
        long id = route.path("routeId").asLong();
        assertRejected(request("PATCH", "/api/routes/" + id,
                Map.of("estimatedFlightTimeHours", 0.0)), "Zero duration");
        assertEquals(route.path("estimatedFlightTimeHours"),
                get("/api/routes/" + id).path("estimatedFlightTimeHours"));
    }

}
