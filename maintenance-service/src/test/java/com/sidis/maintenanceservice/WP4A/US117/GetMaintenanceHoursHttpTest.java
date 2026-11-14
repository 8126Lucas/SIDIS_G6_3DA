
package com.sidis.maintenanceservice.WP4A.US117;

import com.fasterxml.jackson.databind.JsonNode;
import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class GetMaintenanceHoursHttpTest extends GatewayHttpTest {
    @Test
    void shouldReturnTotalMaintenanceHoursByAircraft() throws Exception {
        JsonNode created = createRecord();
        String aircraft = created.path("aircraftRegistration").asText();
        JsonNode response = get("/api/maintenance/reports/hours");
        assertTrue(response.isArray());
        JsonNode row = findRow(response, "aircraftRegistration", aircraft);
        assertTrue(row.path("totalMaintenanceHours").isNumber());
        assertEquals(2.0, row.path("totalMaintenanceHours").asDouble(), 0.001);
    }

    @Test
    void shouldNotReturnAircraftWithoutMaintenanceRecords() throws Exception {
        String aircraft = createAircraft();
        JsonNode response = get("/api/maintenance/reports/hours");
        assertTrue(response.isArray());
        boolean aircraftExists = StreamSupport.stream(response.spliterator(), false).anyMatch(row -> row.path("aircraftRegistration").asText().equals(aircraft));
        assertFalse(aircraftExists,"Aircraft without maintenance records should not be present in the response");
    }
}
