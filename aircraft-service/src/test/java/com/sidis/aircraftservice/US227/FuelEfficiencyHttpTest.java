package com.sidis.aircraftservice.US227;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FuelEfficiencyHttpTest extends GatewayHttpTest {

    @Test void shouldReturnFuelEfficiencyPerAircraft() throws Exception {
        String registration = createAircraft();
        var rows = get("/api/hangar/analytics/fuel-efficiency/aircraft");
        assertTrue(rows.isArray());
        var row = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(r -> registration.equals(r.path("registrationNumber").asText())).findFirst().orElseThrow();
        assertEquals(3.94, row.path("fuelConsumptionLitPerKm").asDouble(), 0.001);
    }
    @Test void shouldReturnFuelEfficiencyPerRoute() throws Exception {
        assertRouteFuelEfficiency();
    }
    @Disabled("Requires an empty deployment; HTTP API does not expose fleet deletion.")
    @Test void shouldReturnEmptyListWhenNoAircraft() {}
    @Disabled("Requires an empty deployment; HTTP API does not expose flight deletion.")
    @Test void shouldReturnEmptyListWhenNoFlights() {}
}
