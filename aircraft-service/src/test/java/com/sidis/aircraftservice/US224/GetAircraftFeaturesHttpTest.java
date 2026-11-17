
package com.sidis.aircraftservice.US224;

import com.sidis.aircraftservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetAircraftFeaturesHttpTest extends OperationalHttpTest {
    @Test
    void shouldReturnAircraftsWithSpecifiedFeatures() throws Exception {
        var body = featuresBody(List.of("Wi-Fi", "USB Charging"));
        var response = request("GET","/api/hangar/aircraft-features",body);
        assertEquals(200, response.statusCode());

        var aircrafts = JSON.readTree(response.body());
        assertNotNull(aircrafts);
        assertTrue(aircrafts.isArray());

        for (var aircraft : aircrafts) {
            var features = aircraft.path("features");
            assertTrue(features.isArray());
            assertTrue(features.toString().contains("Wi-Fi"));
            assertTrue(features.toString().contains("USB Charging"));
        }
    }

    @Test
    void shouldReturnEmptyListWhenNoAircraftHasSpecifiedFeatures() throws Exception {
        var body = featuresBody(List.of("Non"));
        var response = request("GET", "/api/hangar/aircraft-features", body);
        assertEquals(200, response.statusCode());

        var aircrafts = JSON.readTree(response.body());

        assertNotNull(aircrafts);
        assertTrue(aircrafts.isArray());
        assertTrue(aircrafts.isEmpty());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {
    }
}
