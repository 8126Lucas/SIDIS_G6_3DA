package com.sidis.maintenanceservice;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceServiceApplicationTests extends GatewayHttpTest {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        authenticate("technician", "technician123");
        assertTrue(get("/api/maintenance/templates").isContainerNode());
    }
}
