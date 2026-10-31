package com.sidis.routeservice.WP3A.US110;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US110 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class CreateRouteHttpTest extends OperationalHttpTest {

    @Test void shouldCreateRouteWithRegisteredAirportsAndRequirements() throws Exception {
        String origin = createAirport(), destination = createAirport();
        var body = routeBody(origin, destination);
        authenticateAs("atcc");
        var created = expect(201, request("POST", "/api/routes", body));
        assertTrue(created.path("routeId").asLong() > 0);
        var route = get("/api/routes/" + created.path("routeId").asLong());
        assertEquals(body.get("routeName"), route.path("routeName").asText());
        assertEquals(origin, route.path("originIataCode").asText());
        assertEquals(destination, route.path("destinationIataCode").asText());
        assertEquals(2.0, route.path("estimatedFlightTimeHours").asDouble());
        assertEquals(1000.0, route.path("routeRequirements").path("requiredRange").asDouble());
        assertEquals(100, route.path("routeRequirements").path("requiredCapacity").asInt());
        assertEquals("ACTIVE", route.path("status").asText());
    }

    @Test void shouldRejectUnknownOrigin() throws Exception {
        String destination = createAirport(), unknown = unusedIata();
        authenticateAs("atcc");
        assertRejected(request("POST", "/api/routes", routeBody(unknown, destination)), "Unknown origin");
    }

    @Test void shouldRejectUnknownDestination() throws Exception {
        String origin = createAirport(), unknown = unusedIata();
        authenticateAs("atcc");
        assertRejected(request("POST", "/api/routes", routeBody(origin, unknown)), "Unknown destination");
    }

    @Test void shouldRejectIdenticalAirports() throws Exception {
        String code = createAirport();
        authenticateAs("atcc");
        assertRejected(request("POST", "/api/routes", routeBody(code, code)), "Identical endpoints");
    }

    @Test void shouldRejectNonPositiveFlightTime() throws Exception {
        String origin = createAirport(), destination = createAirport();
        var body = routeBody(origin, destination);
        body.put("estimatedFlightTimeHours", -1.0);
        authenticateAs("atcc");
        assertRejected(request("POST", "/api/routes", body), "Negative duration");
    }

    @Test void shouldRejectMissingRequirements() throws Exception {
        String origin = createAirport(), destination = createAirport();
        var body = routeBody(origin, destination);
        body.remove("requirements");
        authenticateAs("atcc");
        assertRejected(request("POST", "/api/routes", body), "Missing aircraft requirements");
    }

}
