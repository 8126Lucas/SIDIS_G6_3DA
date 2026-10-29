package com.sidis.maintenanceservice.WP4A.US116;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class LogMaintenanceActivityUseCaseToRepoTest extends GatewayHttpTest {

    @Test void shouldPersistMaintenanceRecordThroughHttp() throws Exception {
        var record = createRecord();
        var saved = get("/api/maintenance/records/" + record.path("id").asLong());
        assertEquals(record, saved);
        assertEquals("INLINE", saved.path("status").asText());
        assertEquals(2.0, saved.path("durationHours").asDouble());
        assertEquals("HTTP test inspection", saved.path("description").asText());
    }
}
