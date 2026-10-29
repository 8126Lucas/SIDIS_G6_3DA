package com.sidis.aircraftservice.US227;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class FuelEfficiencyUseCaseTest extends GatewayHttpTest {

    @Test void shouldCalculateFuelConsumptionRatePerAircraft() throws Exception {
        String registration = createAircraft();
        var rows = get("/api/hangar/analytics/fuel-efficiency/aircraft");
        var row = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(r -> registration.equals(r.path("registrationNumber").asText()))
                .findFirst().orElseThrow();
        assertEquals(24210.0, row.path("fuelCapacityLiters").asDouble());
        assertEquals(6150.0, row.path("maxRangeKm").asDouble());
        assertEquals(3.94, row.path("fuelConsumptionLitPerKm").asDouble(), 0.001);
    }
    @Test void shouldSortAircraftByFuelConsumptionAscending() throws Exception {
        String regular = createAircraft();
        String economicalModel = unique();
        var body = new java.util.HashMap<String, Object>(modelBody(economicalModel));
        body.put("specs", Map.of("fuelCapacityLiters", 10000.0, "maximumRangeKm", 5000.0,
                "cruisingSpeedKph", 800.0, "standardSeatingCapacity", 150));
        expect(201, request("POST", "/api/catalog/model", body));
        String economical = createAircraft(economicalModel);
        var rows = get("/api/hangar/analytics/fuel-efficiency/aircraft");
        assertTrue(rows.isArray());
        int regularIndex = -1;
        int economicalIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            assertTrue(rows.get(i).path("fuelConsumptionLitPerKm").isNumber());
            String registration = rows.get(i).path("registrationNumber").asText();
            if (regular.equals(registration)) regularIndex = i;
            if (economical.equals(registration)) {
                economicalIndex = i;
                assertEquals(2.0, rows.get(i).path("fuelConsumptionLitPerKm").asDouble(), 0.001);
            }
        }
        assertTrue(regularIndex >= 0, "The created regular aircraft must appear");
        assertTrue(economicalIndex >= 0, "The created economical aircraft must appear");
        assertTrue(economicalIndex < regularIndex, "The lower consumption aircraft must precede the other");
        for (int i = 1; i < rows.size(); i++) {
            assertTrue(rows.get(i - 1).path("fuelConsumptionLitPerKm").asDouble()
                    <= rows.get(i).path("fuelConsumptionLitPerKm").asDouble());
        }
    }
    @Test void shouldCalculateRouteEfficiencyFromFlights() throws Exception {
        assertRouteFuelEfficiency();
    }
    @Disabled("Requires an empty deployment; HTTP API does not expose fleet deletion.")
    @Test void shouldReturnEmptyWhenNoAircraft() {}
    @Disabled("Requires an empty deployment; HTTP API does not expose flight deletion.")
    @Test void shouldReturnEmptyRouteEfficiencyWhenNoFlights() {}
    @Disabled("The public model creation API requires specs; cannot create this internal state through HTTP.")
    @Test void shouldHandleAircraftWithNullSpecs() {}
}
