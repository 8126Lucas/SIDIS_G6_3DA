package com.sidis.discoveryservice;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryServiceApplicationTests {
    @Test
    void shouldRespondThroughHttp() throws Exception {
        String baseUrl = System.getenv().getOrDefault("SIDIS_DISCOVERY_URL", "http://localhost:8761");
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + "/eureka/apps"))
                    .timeout(Duration.ofSeconds(30)).header("Accept", "application/json").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.body().contains("\"applications\""), response.body());
        }
    }
}
