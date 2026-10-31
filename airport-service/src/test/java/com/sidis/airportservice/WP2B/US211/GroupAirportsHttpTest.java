package com.sidis.airportservice.WP2B.US211;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US211 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class GroupAirportsHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"country", "region"})
    void shouldGroupByUsCriteria(String criterion) throws Exception {
        String code = createAirport();
        String key = get("/api/airports/" + code).path("location").path(criterion).asText();
        authenticateAs("atcc");
        var groups = get("/api/airports/grouped?by=" + criterion);
        assertTrue(groups.isObject());
        var group = groups.path(key);
        find(group, "iataCode", code);
        group.forEach(a -> assertEquals(key, a.path("location").path(criterion).asText()));
    }

}
