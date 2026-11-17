package com.sidis.aircraftservice.WP1B.US202;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddModelWithImageHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturn201WhenModelIsCreatedWithImage() throws Exception {
        String model = createModel();
        var response = get("/api/catalog/" + model);
        assertEquals(model, response.path("modelName").asText());
    }

    @Test
    void shouldReturn201WhenModelIsCreatedWithoutImage() throws Exception {
        String model = createModelWithoutImage();
        var response = get("/api/catalog/" + model);
        assertEquals(model, response.path("modelName").asText());
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
