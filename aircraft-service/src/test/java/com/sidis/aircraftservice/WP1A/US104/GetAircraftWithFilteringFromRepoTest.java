package com.sidis.aircraftservice.WP1A.US104;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class GetAircraftWithFilteringFromRepoTest extends GatewayHttpTest {

    @Test void shouldReturnFilteredAircrafts() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        var rows = get("/api/hangar/aircraft?model=" + model
                + "&status=AVAILABLE&manufacturingDate=2020-01-01");
        assertTrue(rows.isArray());
        assertEquals(1, rows.size());
        assertCreatedAircraft(rows.get(0), registration, model);
    }
}
