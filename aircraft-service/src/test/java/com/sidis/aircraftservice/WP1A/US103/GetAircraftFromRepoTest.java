package com.sidis.aircraftservice.WP1A.US103;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class GetAircraftFromRepoTest extends GatewayHttpTest {

    @Test void shouldReturnAircraftWhenAircraftExists() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        assertCreatedAircraft(get("/api/hangar/aircraft/" + registration), registration, model);
    }
}
