package com.sidis.aircraftservice.WP1B.US201;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateAircraftSpecificationHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturn200WhenAircraftSpecsAreUpdated() throws Exception {
        String model = createModel();
        var postedModel = get("/api/catalog/" + model);
        Map<String, Object> body = Map.of(
                "modelName", model,
                "specs", Map.of(
                        "fuelCapacityLiters", 1000.0,
                        "maximumRangeKm", 10000.0,
                        "cruisingSpeedKph", 300.0,
                        "standardSeatingCapacity", 60
                )
        );
        assertEquals(200, patch("/api/catalog/model/" + model, body));
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
