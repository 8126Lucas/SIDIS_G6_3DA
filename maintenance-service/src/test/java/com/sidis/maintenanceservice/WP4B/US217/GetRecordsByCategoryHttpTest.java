
package com.sidis.maintenanceservice.WP4B.US217;

import com.fasterxml.jackson.databind.JsonNode;
import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetRecordsByCategoryHttpTest extends GatewayHttpTest {
    
    @Test
    void shouldReturn200AfterCategoryAlteration() throws Exception {
        var created = createRecord();
        long recordId = created.path("id").asLong();
        JsonNode original = get("/api/maintenance/records/" + recordId);
        String originalAttribute = original.path("template").path("attribute").asText();
        var patchResponse = request("PATCH", "/api/maintenance/records/" + recordId, Map.of("attribute", "AVIONICS"));
        assertEquals(200, patchResponse.statusCode());
        JsonNode updated = get("/api/maintenance/records/" + recordId);
        assertEquals(recordId, updated.path("id").asLong());
        assertEquals("AVIONICS", updated.path("template").path("attribute").asText());
        assertNotEquals(originalAttribute, updated.path("template").path("attribute").asText());
    }

    @Test
    void shouldReturn400WhenMaintenanceAttributeIsInvalid() throws Exception {
        var created = createRecord();
        long recordId = created.path("id").asLong();
        JsonNode original = get("/api/maintenance/records/" + recordId);
        String originalAttribute = original.path("template").path("attribute").asText();
        var patchResponse = request("PATCH", "/api/maintenance/records/" + recordId, Map.of("attribute", "INVALID_CATEGORY"));
        assertEquals(400, patchResponse.statusCode());
        JsonNode afterRequest = get("/api/maintenance/records/" + recordId);
        assertEquals(originalAttribute, afterRequest.path("template").path("attribute").asText());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {
    }
}
