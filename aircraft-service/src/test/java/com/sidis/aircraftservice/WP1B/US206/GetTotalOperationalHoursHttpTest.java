package com.sidis.aircraftservice.WP1B.US206;

import com.sidis.aircraftservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetTotalOperationalHoursHttpTest extends OperationalHttpTest {
    @Test
    void shouldReturnTotalOperationalHoursBySpecifiedAircraft() throws Exception {
        var response=get("/api/hangar/analytics/operational-hours");
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
