package com.sidis.routeservice.WP3A.US111;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US111 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class RouteHistoryHttpTest extends OperationalHttpTest {

    @Test void shouldExposeInitialHistory() throws Exception {
        var route = createRoute();
        var history = get("/api/routes/" + route.path("routeId").asLong() + "/history");
        assertEquals(LocalDate.now().toString(), history.path("routeBegin").asText());
        assertEquals(0, history.path("routeUsage").asInt());
        assertTrue(history.path("routeFinish").isNull());
    }

    @Test void shouldIncreaseUsageAfterScheduledFlight() throws Exception {
        var route = createRoute();
        long id = route.path("routeId").asLong();
        String registration = createAircraft();
        int before = get("/api/routes/" + id + "/history").path("routeUsage").asInt();
        schedule(id, registration, departure());
        assertEquals(before + 1, get("/api/routes/" + id + "/history").path("routeUsage").asInt(),
                "US111: a route use must be recorded in its history");
    }

}
