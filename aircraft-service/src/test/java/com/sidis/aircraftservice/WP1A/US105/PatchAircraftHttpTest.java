package com.sidis.aircraftservice.WP1A.US105;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PatchAircraftHttpTest extends GatewayHttpTest {

    @Test void shouldReturn200WhenAvailabilityChangesSuccessfully() throws Exception {
        String registration = createAircraft();
        var expected = get("/api/hangar/aircraft/" + registration).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) expected).put("status", "MAINTENANCE");
        expect(200, request("PATCH", "/api/hangar/aircraft/" + registration,
                Map.of("registrationNumber", registration, "availability", "MAINTENANCE")));
        assertEquals(expected, get("/api/hangar/aircraft/" + registration),
                "Changing availability must preserve the other aircraft fields");
    }
    @Test void shouldReturn404WhenAircraftDoesNotExist() throws Exception {
        String registration = unique();
        expect(404, request("PATCH", "/api/hangar/aircraft/" + registration,
                Map.of("registrationNumber", registration, "availability", "MAINTENANCE")));
    }
    @Disabled("A real gateway request cannot inject a database failure.")
    @Test void shouldReturn500WhenServiceFails() {}
}
