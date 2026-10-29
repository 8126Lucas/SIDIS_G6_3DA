package com.sidis.maintenanceservice.WP4B.US222;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MaintenanceAlertHttpTest extends GatewayHttpTest {

    @Test void shouldReturnAlertsWithDefaultThresholds() throws Exception {
        String registration = createAircraft();
        var rows = get("/api/maintenance/alerts");
        assertTrue(rows.isArray());
        assertTrue(java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .anyMatch(r -> registration.equals(r.path("registrationNumber").asText())));
    }
    @Test void shouldAcceptCustomThresholds() throws Exception {
        String registration = createAircraft();
        var rows = get("/api/maintenance/alerts?calendarDaysThreshold=90&flightHoursThreshold=300");
        assertTrue(rows.isArray());
        assertTrue(java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .anyMatch(r -> registration.equals(r.path("registrationNumber").asText())));
    }
    @Disabled("Requires control of all aircraft and completed maintenance; no reset API is exposed.")
    @Test void shouldReturnEmptyWhenNoAlerts() {}
}
