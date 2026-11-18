package com.sidis.aircraftservice.WP1A.US103;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GetAircraftHttpTest extends GatewayHttpTest {

    @Test void shouldReturnAircraftWhenExists() throws Exception {
        authenticate("backoffice","backoffice123");
        String model = createModel();

        authenticate("atcc","atcc123");
        String registration = createAircraft(model);
        var aircraft = get("/api/hangar/aircraft/" + registration);

        assertCreatedAircraft(aircraft, registration, model);
        assertEquals(registration, aircraft.path("registrationNumber").asText());
        assertEquals(model, aircraft.path("modelName").asText());
        assertEquals("2020-01-01", aircraft.path("manufacturingDate").asText());
        assertEquals(800.0, aircraft.path("totalFlightHours").asDouble());
    }
    @Test void shouldReturn404WhenAircraftNotFound() throws Exception {
        authenticate("atcc","atcc123");
        expect(404, request("GET", "/api/hangar/aircraft/" + unique(), null));
    }
}
