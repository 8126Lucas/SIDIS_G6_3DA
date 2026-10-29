package com.sidis.aircraftservice.WP1A.US102;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class AddAircraftUseCaseToRepoTest extends GatewayHttpTest {

    @Test void shouldPersistAircraftThroughHttp() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        var saved = get("/api/hangar/aircraft/" + registration);
        assertCreatedAircraft(saved, registration, model);
        assertEquals(registration, saved.path("registrationNumber").asText());
        assertEquals(model, saved.path("modelName").asText());
        assertEquals("2020-01-01", saved.path("manufacturingDate").asText());
        assertEquals("AVAILABLE", saved.path("status").asText());
        assertEquals(1000.0, saved.path("totalOperationalHours").asDouble());
        assertEquals(800.0, saved.path("totalFlightHours").asDouble());
    }
}
