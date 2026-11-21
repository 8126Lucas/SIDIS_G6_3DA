package com.sidis.maintenanceservice.WP4A.US117;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import java.lang.Thread;
import static org.junit.jupiter.api.Assertions.*;

class GetMaintenanceTemplateHttpTest extends GatewayHttpTest {

    @Test void shouldGetTemplateByIdSuccessfully() throws Exception {
        var created = createTemplate();
        Thread.sleep(1000);
        assertEquals(created, get("/api/maintenance/templates/" + created.path("templateId").asLong()));
    }
    @Test void shouldListTemplates() throws Exception {
        var created = createTemplate();
        Thread.sleep(1000);
        var rows = get("/api/maintenance/templates");
        assertTrue(rows.isArray());
        assertTrue(java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .anyMatch(created::equals));
    }
    @Test void shouldReturn404WhenTemplateNotFound() throws Exception {
        authenticate("technician", "technician123");
        expect(404, request("GET", "/api/maintenance/templates/9223372036854775807", null));
    }
}
