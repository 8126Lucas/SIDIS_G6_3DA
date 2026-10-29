package com.sidis.maintenanceservice.US226;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PartsInventoryHttpTest extends GatewayHttpTest {

    @Test void shouldReturnPartsInventorySummary() throws Exception {
        var rows = get("/api/maintenance/parts/inventory");
        assertBootstrapInventory(rows);
        assertTrue(rows.isArray());
        rows.forEach(r -> {
            assertTrue(r.path("partSerialNumber").isTextual());
            assertTrue(r.path("totalQuantityUsed").isNumber());
            assertTrue(r.path("totalCost").isNumber());
            assertTrue(r.path("timesUsedInRecords").isNumber());
        });
    }
    @Test void shouldReturnLowStockAlertsWithDefaultThreshold() throws Exception {
        assertTrue(get("/api/maintenance/parts/low-stock").isArray());
    }
    @Test void shouldAcceptCustomThreshold() throws Exception {
        var rows = get("/api/maintenance/parts/low-stock?threshold=5");
        assertTrue(rows.isArray());
        rows.forEach(r -> assertTrue(r.path("totalQuantityUsed").asInt() >= 5));
    }
    @Disabled("No API to delete records or set used parts; requires an empty deployment.")
    @Test void shouldReturnEmptyInventoryWhenNoPartsUsed() {}
    @Disabled("No API to control used parts across the deployment.")
    @Test void shouldReturnEmptyAlertsWhenNoLowStock() {}
}
