package com.sidis.aircraftservice.support;

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

    protected void assertCreatedAircraft(JsonNode aircraft, String registration, String model) {
        assertEquals(registration, aircraft.path("registrationNumber").asText());
        assertEquals(model, aircraft.path("modelName").asText());
        assertEquals("2020-01-01", aircraft.path("manufacturingDate").asText());
        assertEquals("AVAILABLE", aircraft.path("status").asText());
        assertTrue(aircraft.path("totalOperationalHours").isNumber());
        assertTrue(aircraft.path("totalFlightHours").isNumber());
        assertEquals(1000.0, aircraft.path("totalOperationalHours").asDouble());
        assertEquals(800.0, aircraft.path("totalFlightHours").asDouble());
    }

    protected void assertRouteFuelEfficiency() throws Exception {
        var rows = get("/api/hangar/analytics/fuel-efficiency/routes");
        assertTrue(rows.isArray(), rows.toString());
        assertFalse(rows.isEmpty(), "Requires the seeded flights and routes; an empty report does not test a calculation");
        for (var row : rows) {
            assertTrue(row.path("routeId").isIntegralNumber(), row.toString());
            assertFalse(row.path("routeName").asText().isBlank(), row.toString());
            assertTrue(row.path("distanceKm").isNumber(), row.toString());
            assertTrue(row.path("distanceKm").asDouble() > 0, row.toString());
            assertTrue(row.path("avgFuelConsumptionLitPerKm").isNumber(), row.toString());
            assertTrue(row.path("avgFuelConsumptionLitPerKm").asDouble() > 0, row.toString());
            assertTrue(row.path("estimatedFuelLiters").isNumber(), row.toString());
            double expected = Math.round(row.path("avgFuelConsumptionLitPerKm").asDouble()
                    * row.path("distanceKm").asDouble() * 100.0) / 100.0;
            assertEquals(expected, row.path("estimatedFuelLiters").asDouble(), 0.001, row.toString());
        }
        // Flight bootstrap assigns CS-TUA and D-ABXA (737-800) to route 1.
        var seeded = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> row.path("routeId").asLong() == 1L).toList();
        assertEquals(1, seeded.size(), "Expected the seeded route 1 exactly once");
        var row = seeded.get(0);
        assertEquals(Math.round(70500.0 / 5436.0 * 100.0) / 100.0,
                row.path("avgFuelConsumptionLitPerKm").asDouble(), 0.001);
    }
}
