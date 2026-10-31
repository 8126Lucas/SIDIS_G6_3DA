package com.sidis.routeservice.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP fixtures only. No repositories, mocks, shared bootstrap IDs or direct DB access.
 * Generated with OpenAI Codex; reviewed against the PSOFT US and the public API.
 * Created data remains in the running test deployment (there is no deletion API).
 */
public abstract class OperationalHttpTest extends GatewayHttpTest {
    protected String unique() { return "HTTP-" + UUID.randomUUID(); }

    protected String unusedIata() throws Exception {
        Set<String> used = new HashSet<>();
        get("/api/airports").forEach(a -> used.add(a.path("iataCode").asText()));
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 3; i++)
                code.append((char) ('A' + ThreadLocalRandom.current().nextInt(26)));
            if (!used.contains(code.toString())) return code.toString();
        }
        throw new AssertionError("Could not allocate a free IATA code");
    }

    protected Map<String, Object> airportBody(String code) {
        return new HashMap<>(Map.of(
                "iataCode", code, "airportType", "International",
                "name", unique(), "status", "OPERATIONAL",
                "location", Map.of("city", unique(), "longitude", -9.13,
                        "latitude", 38.77, "region", unique(),
                        "timezone", "Europe/Lisbon", "country", unique()),
                "facilities", Map.of("terminalCount", 2, "gateCount", 12,
                        "services", List.of("fuel", "maintenance")),
                "routeDistance", 0.0,
                "runways", List.of(Map.of("runwayName", "03/21", "length", 3000.0,
                        "orientation", "NE/SW", "status", "OPEN")),
                "contacts", List.of(Map.of("type", "EMAIL", "value", "ops@example.com")),
                "airportPhotos", List.of("https://example.com/airport.jpg")));
    }

    protected String createAirport() throws Exception {
        authenticateAs("backoffice");
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = unusedIata();
            var response = request("POST", "/api/airports", airportBody(code));
            // Retry only a genuine IATA collision with another HTTP test.
            if (response.statusCode() == 409
                    && JSON.readTree(response.body()).path("message").asText().equals("Airport already exists"))
                continue;
            expect(201, response);
            return code;
        }
        throw new AssertionError("IATA collisions exhausted fixture retries");
    }

    protected Map<String, Object> routeBody(String origin, String destination) {
        return new HashMap<>(Map.of("routeName", unique(), "originIataCode", origin,
                "destinationIataCode", destination, "estimatedFlightTimeHours", 2.0,
                "type", "DIRECT", "requirements",
                Map.of("requiredRange", 1000.0, "requiredCapacity", 100)));
    }

    protected JsonNode createRoute(String origin, String destination) throws Exception {
        authenticateAs("atcc");
        return expect(201, request("POST", "/api/routes", routeBody(origin, destination)));
    }

    protected JsonNode createRoute() throws Exception {
        return createRoute(createAirport(), createAirport());
    }

    protected String createAircraft() throws Exception {
        authenticateAs("backoffice");
        String model = unique();
        expect(201, request("POST", "/api/catalog/model", Map.of(
                "modelName", model, "manufacturer", "Airbus",
                "modelImage", List.of("https://example.com/aircraft.jpg"),
                "specs", Map.of("fuelCapacityLiters", 24210.0, "maximumRangeKm", 6150.0,
                        "cruisingSpeedKph", 828.0, "standardSeatingCapacity", 180))));
        authenticateAs("atcc");
        String registration = unique();
        expect(201, request("POST", "/api/hangar/aircraft", Map.of(
                "registrationNumber", registration, "modelName", model,
                "manufacturingDate", "2020-01-01", "totalOperationalHours", 1000.0,
                "totalFlightHours", 800.0, "availability", "AVAILABLE")));
        return registration;
    }

    protected Map<String, Object> flightBody(long routeId, String registration,
                                              LocalDateTime departure) {
        return new HashMap<>(Map.of("routeId", routeId, "aircraftRegistrationNumber", registration,
                "scheduledDeparture", departure.toString(),
                "scheduledArrival", departure.plusHours(2).toString()));
    }

    protected JsonNode schedule(long routeId, String registration, LocalDateTime departure) throws Exception {
        authenticateAs("atcc");
        return expect(201, request("POST", "/api/flights/scheduled",
                flightBody(routeId, registration, departure)));
    }

    protected LocalDateTime departure() {
        return LocalDateTime.now().plusMonths(2).withNano(0);
    }

    protected JsonNode find(JsonNode rows, String field, String value) {
        assertTrue(rows.isArray(), rows.toString());
        return java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> value.equals(row.path(field).asText())).findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + field + "=" + value + " in " + rows));
    }

    protected void assertRejectedFor(java.net.http.HttpResponse<String> response, String messagePart) throws Exception {
        assertRejected(response, messagePart);
        assertTrue(JSON.readTree(response.body()).path("message").asText()
                        .toLowerCase(Locale.ROOT).contains(messagePart.toLowerCase(Locale.ROOT)),
                "Rejection must concern " + messagePart + ": " + response.body());
    }

    protected void assertRejected(java.net.http.HttpResponse<String> response, String reason) throws Exception {
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                reason + ": expected a client/domain error, got " + response.statusCode() + ": " + response.body());
        assertFalse(JSON.readTree(response.body()).path("message").asText().isBlank(),
                "A rejected request must explain the error");
    }
}
