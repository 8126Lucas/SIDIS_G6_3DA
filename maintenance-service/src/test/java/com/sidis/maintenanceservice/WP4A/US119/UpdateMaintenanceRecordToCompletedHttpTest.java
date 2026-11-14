package com.sidis.maintenanceservice.WP4A.US119;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateMaintenanceRecordToCompletedHttpTest extends GatewayHttpTest {
    @Test
    void shouldUpdateToCompletedARecord() throws Exception {
        var created = createRecord();
        long recordId = created.path("id").asLong();
        var body = completeRecordBody();
        var response = request("PATCH", "/api/maintenance/records/" + recordId + "/complete", body);
        assertEquals(200, response.statusCode(), response.body());
        var patchResponse = JSON.readTree(response.body());
        assertEquals(recordId, patchResponse.path("id").asLong());
        assertEquals("COMPLETED", patchResponse.path("status").asText());
        var updated = get("/api/maintenance/records/" + recordId);
        assertEquals(recordId, updated.path("id").asLong());
        assertEquals("COMPLETED", updated.path("status").asText());
        assertEquals(body.get("notes"), updated.path("notes").asText());
        assertEquals(JSON.valueToTree(body.get("tasksDone")), updated.path("tasksDone"));
        assertEquals(body.get("endDate"), updated.path("endDate").asText());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {
    }
}