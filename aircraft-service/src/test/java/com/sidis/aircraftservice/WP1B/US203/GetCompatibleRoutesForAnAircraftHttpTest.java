package com.sidis.aircraftservice.WP1B.US203;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetCompatibleRoutesForAnAircraftHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturn200EvenWithCompatibleRoutes() throws Exception {
        String model = createModel(); // capacity: 180; range: 6150
        String aircraftInstance = createAircraft(model); // capacity: 140; meanRange: 500

        var response = get("/api/catalog/" + model);
        assertEquals(model, response.path("modelName").asText());
        get("/api/hangar/aircraft/" + aircraftInstance + "/compatible-routes");
    }

    @Test
    void shouldReturn404WhenAircraftDoesNotExist() throws Exception {
        var response = expect(404, request("GET", "/api/hangar/aircraft/nan/compatible-routes", null));
        assertEquals(404, response.path("status").asInt());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
