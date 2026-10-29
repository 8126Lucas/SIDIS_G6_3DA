package com.sidis.aircraftservice.WP1A.US104;

import com.sidis.aircraftservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GetAircraftWithFilteringHttpTest extends GatewayHttpTest {

    private String filter(String model, String status, String date) {
        return "/api/hangar/aircraft?model=" + model + "&status=" + status
                + "&manufacturingDate=" + date + "&size=100";
    }
    @Test void shouldReturnAircraftWhenAllFiltersMatch() throws Exception {
        String model = createModel();
        String registration = createAircraft(model);
        var aircraft = get(filter(model, "AVAILABLE", "2020-01-01"));
        assertTrue(aircraft.isArray());
        assertEquals(1, aircraft.size());
        assertCreatedAircraft(aircraft.get(0), registration, model);
    }
    @Test void shouldReturnEmptyWhenModelDoesNotMatch() throws Exception {
        createAircraft();
        assertEquals(0, get(filter(unique(), "AVAILABLE", "2020-01-01")).size());
    }
    @Test void shouldReturnEmptyWhenStatusDoesNotMatch() throws Exception {
        String model = createModel();
        createAircraft(model);
        assertEquals(0, get(filter(model, "MAINTENANCE", "2020-01-01")).size());
    }
    @Test void shouldReturnEmptyWhenDateDoesNotMatch() throws Exception {
        String model = createModel();
        createAircraft(model);
        assertEquals(0, get(filter(model, "AVAILABLE", "2000-01-01")).size());
    }
}
