package com.sidis.maintenanceservice.WP4B.US218;

import com.fasterxml.jackson.databind.JsonNode;
import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchMaintenanceRecordsHttpTest extends GatewayHttpTest {
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

    @Test
    void shouldReturnRecordsWithinDateRange() throws Exception {
        JsonNode created = createRecord();
        long recordId = created.path("id").asLong();
        JsonNode response = get("/api/maintenance/records" + "?initialDate=2025-12-31" + "&finalDate=2026-01-02");

        assertTrue(response.isArray());
        assertTrue(response.toString().contains("\"id\":" + recordId), "Expected record to be included in date range");
        assertEquals("2026-01-01", response.get(0).path("startDate").asText());
    }

    @Test
    void shouldReturnRecordsByMaintenanceAttribute() throws Exception {
        JsonNode created = createRecord();
        long recordId = created.path("id").asLong();
        JsonNode response = get("/api/maintenance/records?attribute=ENGINE");

        assertTrue(response.isArray());
        boolean found = false;

        for (JsonNode record : response) {
            if (record.path("id").asLong() == recordId) {
                found = true;
                assertEquals("ENGINE", record.path("template").path("attribute").asText());
            }
        }
        assertTrue(found, "Expected created record in ENGINE results");
    }

    @Test
    void shouldReturnRecordsByUpdatedMaintenanceAttribute() throws Exception {
        JsonNode created = createRecord();
        long recordId = created.path("id").asLong();
        expect(200, request("PATCH", "/api/maintenance/records/" + recordId, Map.of("attribute", "AVIONICS")));
        JsonNode response = get("/api/maintenance/records?attribute=AVIONICS");

        assertTrue(response.isArray());
        boolean found = false;
        for (JsonNode record : response) {
            if (record.path("id").asLong() == recordId) {
                found = true;
                assertEquals("AVIONICS", record.path("template").path("attribute").asText());
            }
        }

        assertTrue(found, "Expected updated record in AVIONICS results");
    }

    @Test
    void shouldReturnEmptyListWhenNoRecordsMatchDateRange() throws Exception {
        createRecord();
        JsonNode response = get("/api/maintenance/records" + "?initialDate=2020-01-01" + "&finalDate=2020-01-02");
        assertTrue(response.isArray());
        assertEquals(0, response.size());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {
    }
}
