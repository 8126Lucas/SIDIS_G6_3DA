package com.sidis.maintenanceservice.WP4A.US116;

import com.fasterxml.jackson.databind.JsonNode;
import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetRecordsForAnAircraftHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturnRecordsByAircraft() throws Exception {
        JsonNode created = createRecord();
        String aircraft = created.path("aircraftRegistration").asText();
        long recordId = created.path("id").asLong();
        JsonNode response = get("/api/maintenance/records?aircraft=" + aircraft);
        assertTrue(response.isArray());
        assertEquals(1, response.size());
        assertEquals(recordId, response.get(0).path("id").asLong());
        assertEquals(aircraft, response.get(0).path("aircraftRegistration").asText());
    }
}
