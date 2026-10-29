package com.sidis.maintenanceservice.WP4B.US219;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class OngoingMaintenanceUseCaseTest extends GatewayHttpTest {

    @Test void shouldReturnOngoingMaintenanceActivities() throws Exception {
        var record = createRecord();
        long id = record.path("id").asLong();
        expect(200, request("PATCH", "/api/maintenance/records/" + id + "/status", Map.of("status", "ONGOING")));
        var rows = get("/api/maintenance/ongoing");
        assertTrue(java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .anyMatch(r -> id == r.path("id").asLong()));
        rows.forEach(r -> assertEquals("ONGOING", r.path("status").asText()));
    }
}
