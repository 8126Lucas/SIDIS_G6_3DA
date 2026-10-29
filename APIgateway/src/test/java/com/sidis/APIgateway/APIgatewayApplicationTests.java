package com.sidis.APIgateway;

import com.sidis.APIgateway.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class APIgatewayApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        assertTrue(get("/api/hangar/analytics/status-summary").isContainerNode());
    }
}
