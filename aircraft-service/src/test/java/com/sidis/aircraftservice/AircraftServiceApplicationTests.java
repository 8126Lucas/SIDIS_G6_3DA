package com.sidis.aircraftservice;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AircraftServiceApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        assertTrue(get("/api/hangar/analytics/status-summary").isContainerNode());
    }
}
