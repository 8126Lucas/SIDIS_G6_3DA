package com.sidis.maintenanceservice.WP4A.US115.US115b;

import com.sidis.maintenanceservice.support.GatewayHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// Persistence is verified through the public API; Java overloads share the same HTTP operation.
class AddMaintenanceTemplateUseCaseToRepoTest extends GatewayHttpTest {

    @Test void shouldPersistMaintenanceTemplateThroughHttp() throws Exception {
        var template = createTemplate();
        var saved = get("/api/maintenance/templates/" + template.path("templateId").asLong());
        assertEquals(template, saved);
        assertEquals("INSPECTION", saved.path("operation").asText());
        assertEquals("ENGINE", saved.path("attribute").asText());
        assertEquals(2.0, saved.path("expectedDuration").asDouble());
    }
}
