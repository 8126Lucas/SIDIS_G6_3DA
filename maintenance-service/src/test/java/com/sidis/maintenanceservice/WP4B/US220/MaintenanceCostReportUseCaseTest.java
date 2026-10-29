package com.sidis.maintenanceservice.WP4B.US220;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class MaintenanceCostReportUseCaseTest extends GatewayHttpTest {

    @Test void shouldReportZeroCostForAircraftWithNoUsedParts() throws Exception {
        var record = createRecord();
        String registration = record.path("aircraftRegistration").asText();
        var rows = get("/api/maintenance/reports/costs?groupBy=aircraft");
        var row = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(r -> registration.equals(r.path("group").asText())).findFirst().orElseThrow();
        assertEquals(0.0, row.path("totalCost").asDouble());
    }
    @Test void shouldReportZeroCostForModelWithNoUsedParts() throws Exception {
        var record = createRecord();
        String registration = record.path("aircraftRegistration").asText();
        String model = get("/api/hangar/aircraft/" + registration).path("modelName").asText();
        var rows = get("/api/maintenance/reports/costs?groupBy=model");
        var row = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(r -> model.equals(r.path("group").asText())).findFirst().orElseThrow();
        assertEquals(0.0, row.path("totalCost").asDouble());
    }
    @Test void shouldCalculateCostsByAircraft() throws Exception {
        assertBootstrapCostsByAircraft();
    }
    @Test void shouldCalculateCostsByAircraftModel() throws Exception {
        assertBootstrapCostsByModel();
    }
}
