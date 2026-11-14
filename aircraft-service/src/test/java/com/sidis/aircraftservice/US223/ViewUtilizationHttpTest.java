
package com.sidis.aircraftservice.US223;

import com.sidis.aircraftservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class ViewUtilizationHttpTest extends OperationalHttpTest {
    @Test
    void shouldReturnAircraftUtilizationHtml() throws Exception {
        HttpResponse<String> response = request("GET", "/api/hangar/view/utilization",null,"text/html");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("<html"));
        assertTrue(response.body().contains("Aircraft Utilization Report"));
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {
    }
}
