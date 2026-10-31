package com.sidis.airportservice.WP2A.US109;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US109 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class AirportStatusHttpTest extends OperationalHttpTest {

    @ParameterizedTest @ValueSource(strings = {"CLOSED", "UNDER_MAINTENANCE", "OPERATIONAL"})
    void shouldPersistOperationalStatus(String status) throws Exception {
        String code = createAirport();
        if (status.equals("OPERATIONAL"))
            expect(200, request("PATCH", "/api/airports/" + code + "/status", Map.of("status", "CLOSED")));
        var changed = expect(200, request("PATCH", "/api/airports/" + code + "/status", Map.of("status", status)));
        assertEquals(status, changed.path("status").asText());
        assertEquals(status, get("/api/airports/" + code).path("status").asText());
    }

    @Test void shouldRejectUnknownStatus() throws Exception {
        String code = createAirport();
        assertRejected(request("PATCH", "/api/airports/" + code + "/status",
                Map.of("status", "NOT_A_STATUS")), "Invalid status");
        assertEquals("OPERATIONAL", get("/api/airports/" + code).path("status").asText());
    }

}
