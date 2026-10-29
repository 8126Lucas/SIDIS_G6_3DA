package com.sidis.maintenanceservice.WP4B.US219;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OngoingMaintenanceHttpTest extends GatewayHttpTest {

    @Test void shouldReturnOngoingMaintenanceActivities() throws Exception {
        var created = createRecord();
        long id = created.path("id").asLong();
        expect(200, request("PATCH", "/api/maintenance/records/" + id + "/status", Map.of("status", "ONGOING")));
        var rows = get("/api/maintenance/ongoing");
        assertTrue(rows.isArray());
        assertTrue(java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .anyMatch(r -> id == r.path("id").asLong()));
        rows.forEach(r -> assertEquals("ONGOING", r.path("status").asText()));
    }
}
