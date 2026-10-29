package com.sidis.aircraftservice.WP1A.US101;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddModelHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturn201WhenModelIsCreated() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        assertEquals(model, get("/api/hangar/aircraft/" + registration).path("modelName").asText());
    }

    @Test
    void shouldReturn400WhenModelNameIsBlank() throws Exception {
        var response = expect(400, request("POST", "/api/catalog/model", modelBody("")));
        assertTrue(response.path("validationErrors").toString().contains("modelName"));
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
