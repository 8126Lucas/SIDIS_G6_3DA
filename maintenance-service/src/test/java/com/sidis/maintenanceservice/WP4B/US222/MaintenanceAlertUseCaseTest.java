package com.sidis.maintenanceservice.WP4B.US222;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class MaintenanceAlertUseCaseTest extends GatewayHttpTest {

    private com.fasterxml.jackson.databind.JsonNode alert(String registration, double threshold) throws Exception {
        var rows = get("/api/maintenance/alerts?calendarDaysThreshold=180&flightHoursThreshold=" + threshold);
        return java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(r -> registration.equals(r.path("registrationNumber").asText())).findFirst().orElseThrow();
    }
    @Test void shouldAlertWhenAircraftHasNoCompletedMaintenance() throws Exception {
        var row = alert(createAircraft(), 10000.0);
        assertTrue(row.path("alertReason").asText().contains("No completed maintenance"));
        assertTrue(row.path("lastMaintenanceDate").isNull());
        assertTrue(row.path("daysSinceLastMaintenance").isNull());
        assertFalse(row.path("alertReason").asText().contains("Total flight hours"),
                "800 hours must not trigger the 10000-hour threshold");
    }
    @Test void shouldAlertWhenFlightHoursExceedThreshold() throws Exception {
        var row = alert(createAircraft(), 500.0);
        assertEquals(800.0, row.path("totalFlightHours").asDouble());
        assertTrue(row.path("alertReason").asText().contains("Total flight hours"));
        assertTrue(row.path("alertReason").asText().contains("threshold of 500"));
    }
    @Test void shouldAlertWhenDaysSinceLastMaintenanceExceedsThreshold() throws Exception {
        // Bootstrap contains completed maintenance for CS-TUA; a high hour limit isolates calendar alerts.
        var rows = get("/api/maintenance/alerts?calendarDaysThreshold=1&flightHoursThreshold=1000000000");
        var row = findRow(rows, "registrationNumber", "CS-TUA");
        assertTrue(row.path("lastMaintenanceDate").isTextual(), row.toString());
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(row.path("lastMaintenanceDate").asText()), java.time.LocalDate.now());
        assertTrue(days >= 1, "The seeded completed maintenance must exceed the calendar threshold");
        assertTrue(row.path("daysSinceLastMaintenance").isIntegralNumber(), row.toString());
        assertEquals(days, row.path("daysSinceLastMaintenance").asLong());
        assertTrue(row.path("alertReason").asText().contains("days ago"));
        assertTrue(row.path("alertReason").asText().contains("threshold: 1"));
        assertFalse(row.path("alertReason").asText().contains("Total flight hours"));
    }
    @Disabled("No public completion endpoint; cannot seed recent completed maintenance.")
    @Test void shouldNotAlertWhenMaintenanceIsRecentAndFlightHoursAreLow() {}
}
