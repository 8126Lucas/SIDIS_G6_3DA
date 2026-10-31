package com.sidis.airportservice.support;

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


    /** Authenticate as the actor named in the US; fixtures may first need BACKOFFICE. */
    protected void authenticateAs(String role) throws Exception {
        String key = role.toUpperCase(java.util.Locale.ROOT);
        var response = request("POST", "/api/auth/login", Map.of(
                "username", System.getenv().getOrDefault("SIDIS_" + key + "_USERNAME", role),
                "password", System.getenv().getOrDefault("SIDIS_" + key + "_PASSWORD", role + "123")));
        var login = expect(200, response);
        token = login.path("token").asText();
        assertFalse(token.isBlank(), "Login must return a token for " + role);
    }

    protected HttpResponse<String> postCsv(String path, String csv) throws Exception {
        String boundary = "sidis-" + UUID.randomUUID();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"airports.csv\"\r\n"
                + "Content-Type: text/csv\r\n\r\n" + csv + "\r\n--" + boundary + "--\r\n";
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    protected JsonNode get(String path) throws Exception {
        return expect(200, request("GET", path, null));
    }

}
