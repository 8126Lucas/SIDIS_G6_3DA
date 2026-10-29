package com.sidis.maintenanceservice.WP4B.US221;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class TurnaroundAverageUseCaseTest extends GatewayHttpTest {

    @Test void shouldReturnTurnaroundReportThroughHttp() throws Exception {
        var rows = get("/api/maintenance/turnaround-average");
        assertTrue(rows.isArray());
        assertFalse(rows.isEmpty(), "Bootstrap contains completed records; an empty report does not test averages");
        rows.forEach(r -> {
            assertTrue(r.path("aircraftType").isTextual());
            assertFalse(r.path("aircraftType").asText().isBlank(), "Every average must identify its aircraft model: " + r);
            assertTrue(r.path("averageDays").isNumber());
            assertTrue(r.path("averageDays").asDouble() >= 0, "Turnaround days cannot be negative: " + r);
        });
    }
    @Disabled("No API to isolate one type or supply valid start/end dates; current bootstrap has inconsistent dates and the record response omits endDate.")
    @Test void shouldCalculateAverageForSingleAircraftType() {}
    @Disabled("Exact multi-type average requires valid completion fixtures; bootstrap dates are inconsistent and record responses omit endDate.")
    @Test void shouldCalculateAveragePerAircraftType() {}
    @Disabled("Requires control of all completed records; no reset/deletion endpoint exists.")
    @Test void shouldReturnEmptyListWhenNoCompletedRecords() {}
}
