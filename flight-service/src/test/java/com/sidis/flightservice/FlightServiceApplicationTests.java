package com.sidis.flightservice;

import com.sidis.flightservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightServiceApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        assertTrue(get("/api/flights/aircraft/HTTP-NONEXISTENT").isContainerNode());
    }
}
