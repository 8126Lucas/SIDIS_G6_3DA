package com.sidis.maintenanceservice.WP4B.US220;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceCostReportHttpTest extends GatewayHttpTest {

    @Test void shouldReturnMaintenanceCostReport() throws Exception {
        assertBootstrapCostsByAircraft();
        assertBootstrapCostsByModel();
        for (String grouping : java.util.List.of("aircraft", "model")) {
            var rows = get("/api/maintenance/reports/costs?groupBy=" + grouping);
            assertTrue(rows.isArray());
            rows.forEach(r -> {
                assertTrue(r.path("group").isTextual());
                assertTrue(r.path("totalCost").isNumber());
                assertTrue(r.path("totalCost").asDouble() >= 0);
            });
        }
    }
}
