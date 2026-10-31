package com.sidis.airportservice.WP2B.US210;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US210 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class BusiestAirportsHttpTest extends OperationalHttpTest {

    @Test void shouldCountBothEndsAndSortByNumberOfRoutes() throws Exception {
        String hub = createAirport();
        createRoute(hub, createAirport());
        createRoute(createAirport(), hub);
        authenticateAs("backoffice");
        var rows = get("/api/airports/busiest");
        assertEquals(2, find(rows, "iataCode", hub).path("numberOfRoutes").asLong());
        for (int i = 1; i < rows.size(); i++)
            assertTrue(rows.get(i - 1).path("numberOfRoutes").asLong()
                    >= rows.get(i).path("numberOfRoutes").asLong(), "Busiest first");
    }

}
