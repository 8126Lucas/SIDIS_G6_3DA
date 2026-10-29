package com.sidis.maintenanceservice.WP4A.US119;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceRecordHttpTest extends GatewayHttpTest {

    @Test void shouldGetRecordByIdSuccessfully() throws Exception {
        var created = createRecord();
        assertEquals(created, get("/api/maintenance/records/" + created.path("id").asLong()));
    }
    @Test void shouldAdvanceStatusSuccessfully() throws Exception {
        var created = createRecord();
        String path = "/api/maintenance/records/" + created.path("id").asLong();
        var updated = expect(200, request("PATCH", path + "/status", Map.of("status", "ONGOING")));
        assertEquals("ONGOING", updated.path("status").asText());
        var expected = created.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) expected).put("status", "ONGOING");
        assertEquals(expected, updated, "Changing status must preserve the record fields");
        assertEquals(expected, get(path), "The complete updated record must persist");
    }
    @Test void shouldReturn409WhenRecordNotFound() throws Exception {
        expect(409, request("GET", "/api/maintenance/records/9223372036854775807", null));
    }
}
