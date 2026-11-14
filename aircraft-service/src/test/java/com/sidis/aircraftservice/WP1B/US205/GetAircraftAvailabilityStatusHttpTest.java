package com.sidis.aircraftservice.WP1B.US205;

import com.sidis.aircraftservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetAircraftAvailabilityStatusHttpTest extends OperationalHttpTest {
    @Test
    void shouldReturnTotalOperationalHoursBySpecifiedAircraft() throws Exception {
        String aircraftRegistration=createAircraft(0.0);
        var response=get("/api/hangar/aircraft/"+aircraftRegistration+"/status");
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void shouldReturn404WhenAircraftDoesntExist() throws Exception {
        String aircraftRegistration=unique();
        String pathHangar="/api/hangar/aircraft/"+aircraftRegistration+"/status";
        var response=request("GET", pathHangar, null);
        assertEquals(404,response.statusCode());
    }

    @Test
    void shouldReturnTheAircraftsAvailability() throws Exception {
        var response=get("/api/hangar/analytics/aircraft-availability");
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void shouldReturnTheAircraftsStatusSummary() throws Exception {
        var response=get("/api/hangar/analytics/status-summary");
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
