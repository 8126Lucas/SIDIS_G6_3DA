package com.sidis.aircraftservice.WP1A.US101;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class AddModelToRepoTest extends GatewayHttpTest {

    @Test void shouldPersistModelThroughHttp() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        assertEquals(model, get("/api/hangar/aircraft/" + registration).path("modelName").asText());
    }
}
