package com.sidis.maintenanceservice.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real gateway tests: start the services before running these tests.
 * Override SIDIS_BASE_URL and SIDIS_BACKOFFICE_USERNAME/PASSWORD as needed.
 * Fixtures use unique names and remain in the test environment (no delete API).
 */
public abstract class GatewayHttpTest {
    protected static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl = System.getenv().getOrDefault("SIDIS_BASE_URL", "https://localhost:7979");
    private HttpClient client;
    private String token;

    @BeforeEach
    protected void login() throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        // Trust the self-signed certificate only for the local development gateway.
        URI gateway = URI.create(baseUrl);
        if ("localhost".equals(gateway.getHost()) || "127.0.0.1".equals(gateway.getHost())) {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            }}, new SecureRandom());
            builder.sslContext(tls);
        }
        client = builder.build();
        HttpResponse<String> response = request("POST", "/api/auth/login", Map.of(
                "username", System.getenv().getOrDefault("SIDIS_BACKOFFICE_USERNAME", "backoffice"),
                "password", System.getenv().getOrDefault("SIDIS_BACKOFFICE_PASSWORD", "backoffice123")));
        assertEquals(200, response.statusCode(), "Gateway login failed (check test credentials).");
        token = JSON.readTree(response.body()).path("token").asText();
        assertFalse(token.isBlank(), "Login must return a token");
    }

    @AfterEach
    protected void closeClient() {
        if (client != null) client.close();
    }

    protected HttpResponse<String> request(String method, String path, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30)).header("Accept", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected JsonNode expect(int status, HttpResponse<String> response) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        return response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
    }

    protected JsonNode get(String path) throws Exception {
        return expect(200, request("GET", path, null));
    }

    protected String unique() { return "HTTP-" + UUID.randomUUID(); }

    protected Map<String, Object> modelBody(String name) {
        return Map.of("modelName", name, "manufacturer", "Airbus",
                "modelImage", List.of("https://example.com/a320.jpg"),
                "specs", Map.of("fuelCapacityLiters", 24210.0, "maximumRangeKm", 6150.0,
                        "cruisingSpeedKph", 828.0, "standardSeatingCapacity", 180));
    }

    protected String createModel() throws Exception {
        String name = unique();
        expect(201, request("POST", "/api/catalog/model", modelBody(name)));
        return name;
    }

    protected Map<String, Object> aircraftBody(String registration, String model) {
        return Map.of("registrationNumber", registration, "modelName", model,
                "manufacturingDate", "2020-01-01", "totalOperationalHours", 1000.0,
                "totalFlightHours", 800.0, "availability", "AVAILABLE");
    }

    protected String createAircraft(String model) throws Exception {
        String registration = unique();
        expect(201, request("POST", "/api/hangar/aircraft", aircraftBody(registration, model)));
        return registration;
    }

    protected String createAircraft() throws Exception { return createAircraft(createModel()); }

    protected JsonNode createTemplate() throws Exception {
        String name = unique();
        JsonNode template = expect(200, request("POST", "/api/maintenance/templates", Map.of(
                "name", name, "expectedDuration", 2.0,
                "templateChecklist", Map.of("Inspect engine", false),
                "operation", "INSPECTION", "attribute", "ENGINE")));
        assertTrue(template.path("templateId").isIntegralNumber());
        assertTrue(template.path("templateId").asLong() > 0);
        assertEquals(name, template.path("name").asText());
        assertEquals(2.0, template.path("expectedDuration").asDouble());
        assertEquals("INSPECTION", template.path("operation").asText());
        assertEquals("ENGINE", template.path("attribute").asText());
        assertEquals(JSON.valueToTree(Map.of("Inspect engine", false)), template.path("templateChecklist"));
        return template;
    }

    protected JsonNode createRecord() throws Exception {
        String registration = createAircraft();
        long templateId = createTemplate().path("templateId").asLong();
        expect(200, request("POST", "/api/maintenance/record", Map.of(
                "registrationNumber", registration, "templateId", templateId,
                "durationHours", 2.0, "description", "HTTP test inspection",
                "startDate", "2026-01-01")));
        JsonNode records = get("/api/maintenance/records?aircraft=" + registration);
        assertTrue(records.isArray());
        assertEquals(1, records.size(), "Expected the record created for this unique aircraft");
        JsonNode record = records.get(0);
        assertTrue(record.path("id").isIntegralNumber());
        assertTrue(record.path("id").asLong() > 0);
        assertEquals(registration, record.path("aircraftRegistration").asText());
        assertEquals(templateId, record.path("template").path("templateId").asLong());
        assertEquals(2.0, record.path("durationHours").asDouble());
        assertEquals("HTTP test inspection", record.path("description").asText());
        assertEquals("2026-01-01", record.path("startDate").asText());
        assertEquals("INLINE", record.path("status").asText());
        return record;
    }

    protected JsonNode findRow(JsonNode rows, String field, String value) {
        assertTrue(rows.isArray(), "Expected a JSON array: " + rows);
        var matches = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> value.equals(row.path(field).asText())).toList();
        assertEquals(1, matches.size(), "Expected exactly one " + field + "=" + value + ": " + rows);
        return matches.get(0);
    }

    // Read-only expectations for the current maintenance Bootstrap. HTTP-created records have no used parts.
    protected void assertBootstrapInventory(JsonNode rows) {
        Map<String, Integer> quantities = Map.of("ENG-001", 2, "TIRE-900", 4,
                "BRAKE-200", 2, "FUEL-PUMP-01", 1, "OIL-FILTER-22", 2);
        Map<String, Double> costs = Map.of("ENG-001", 3000.0, "TIRE-900", 3200.0,
                "BRAKE-200", 2400.0, "FUEL-PUMP-01", 3200.0, "OIL-FILTER-22", 500.0);
        for (var entry : quantities.entrySet()) {
            JsonNode part = findRow(rows, "partSerialNumber", entry.getKey());
            assertTrue(part.path("totalQuantityUsed").isIntegralNumber(), part.toString());
            assertTrue(part.path("totalCost").isNumber(), part.toString());
            assertEquals(entry.getValue().intValue(), part.path("totalQuantityUsed").asInt(), part.toString());
            assertEquals(costs.get(entry.getKey()), part.path("totalCost").asDouble(), 0.001, part.toString());
            assertEquals(1, part.path("timesUsedInRecords").asLong(), part.toString());
        }
    }

    protected void assertBootstrapCostsByAircraft() throws Exception {
        JsonNode rows = get("/api/maintenance/reports/costs?groupBy=aircraft");
        for (var entry : Map.of("CS-TUA", 6700.0, "DA-BXA", 5600.0, "FH-XKA", 0.0).entrySet()) {
            JsonNode row = findRow(rows, "group", entry.getKey());
            assertTrue(row.path("totalCost").isNumber(), row.toString());
            assertEquals(entry.getValue(), row.path("totalCost").asDouble(), 0.001, row.toString());
        }
    }

    protected void assertBootstrapCostsByModel() throws Exception {
        // Resolve models through HTTP; mismatched bootstrap registrations must fail visibly.
        Map<String, Double> expected = new java.util.HashMap<>();
        for (var entry : Map.of("CS-TUA", 6700.0, "DA-BXA", 5600.0, "FH-XKA", 0.0).entrySet()) {
            String model = get("/api/hangar/aircraft/" + entry.getKey()).path("modelName").asText();
            assertFalse(model.isBlank(), "Aircraft must have a model: " + entry.getKey());
            expected.merge(model, entry.getValue(), Double::sum);
        }
        JsonNode rows = get("/api/maintenance/reports/costs?groupBy=model");
        for (var entry : expected.entrySet()) {
            JsonNode row = findRow(rows, "group", entry.getKey());
            assertTrue(row.path("totalCost").isNumber(), row.toString());
            assertEquals(entry.getValue(), row.path("totalCost").asDouble(), 0.001, row.toString());
        }
    }
}
