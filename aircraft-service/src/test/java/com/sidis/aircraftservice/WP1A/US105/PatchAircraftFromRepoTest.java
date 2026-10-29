package com.sidis.aircraftservice.WP1A.US105;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class PatchAircraftFromRepoTest extends GatewayHttpTest {

    @Test void shouldChangeAircraftAvailability() throws Exception {
        String registration = createAircraft();
        var expected = get("/api/hangar/aircraft/" + registration).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) expected).put("status", "MAINTENANCE");
        expect(200, request("PATCH", "/api/hangar/aircraft/" + registration,
                Map.of("registrationNumber", registration, "availability", "MAINTENANCE")));
        assertEquals(expected, get("/api/hangar/aircraft/" + registration),
                "Changing availability must persist and preserve the other aircraft fields");
    }
}
