package com.sidis.routeservice;

import com.sidis.routeservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteServiceApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        assertTrue(get("/api/routes").isContainerNode());
    }
}
