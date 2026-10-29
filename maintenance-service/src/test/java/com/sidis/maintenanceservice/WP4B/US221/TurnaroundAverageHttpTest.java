package com.sidis.maintenanceservice.WP4B.US221;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TurnaroundAverageHttpTest extends GatewayHttpTest {

    @Test void shouldReturnTurnaroundAverages() throws Exception {
        var rows = get("/api/maintenance/turnaround-average");
        assertTrue(rows.isArray());
        assertFalse(rows.isEmpty(), "Bootstrap contains completed records");
        rows.forEach(r -> {
            assertTrue(r.path("aircraftType").isTextual());
            assertFalse(r.path("aircraftType").asText().isBlank(), r.toString());
            assertTrue(r.path("averageDays").isNumber());
            assertTrue(r.path("averageDays").asDouble() >= 0);
        });
    }
    @Disabled("Requires a deployment without completed records; no completion/deletion API is exposed.")
    @Test void shouldReturnEmptyListWhenNoData() {}
}
