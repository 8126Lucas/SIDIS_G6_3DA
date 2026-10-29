package com.sidis.aircraftservice.WP1A.US102;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AddAircraftHttpTest extends GatewayHttpTest {

    @Test void shouldCreateAircraftAndReadItBack() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        var aircraft = get("/api/hangar/aircraft/" + registration);
        assertCreatedAircraft(aircraft, registration, model);
        assertEquals(registration, aircraft.path("registrationNumber").asText());
        assertEquals(model, aircraft.path("modelName").asText());
        assertEquals("AVAILABLE", aircraft.path("status").asText());
        assertEquals(1000.0, aircraft.path("totalOperationalHours").asDouble());
    }
    @Test void shouldReturn404WhenModelNotFound() throws Exception {
        expect(404, request("POST", "/api/hangar/aircraft", aircraftBody(unique(), unique())));
    }
    @Disabled("A real gateway request cannot inject a database failure.")
    @Test void shouldReturn500WhenServiceFails() {}
}
