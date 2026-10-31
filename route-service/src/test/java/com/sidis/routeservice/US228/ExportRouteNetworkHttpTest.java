package com.sidis.routeservice.US228;

import com.sidis.routeservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US228 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class ExportRouteNetworkHttpTest extends OperationalHttpTest {

    @Test void shouldExportGeoJsonWithCoordinatesAndCreatedRoute() throws Exception {
        var route = createRoute();
        authenticateAs("backoffice");
        var response = request("GET", "/api/routes/export/geojson", null);
        var document = expect(200, response);
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/geo+json"));
        assertEquals("FeatureCollection", document.path("type").asText());
        var feature = java.util.stream.StreamSupport.stream(document.path("features").spliterator(), false)
                .filter(f -> f.path("properties").path("routeId").equals(route.path("routeId")))
                .findFirst().orElseThrow();
        assertEquals("LineString", feature.path("geometry").path("type").asText());
        var coordinates = feature.path("geometry").path("coordinates");
        assertEquals(2, coordinates.size());
        for (var point : coordinates) {
            assertEquals(-9.13, point.get(0).asDouble(), 0.0001);
            assertEquals(38.77, point.get(1).asDouble(), 0.0001);
        }
    }

    @Test void shouldExportWellFormedKmlWithCreatedRoute() throws Exception {
        var route = createRoute();
        authenticateAs("backoffice");
        var response = request("GET", "/api/routes/export/kml", null);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .contains("application/vnd.google-earth.kml+xml"));
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = assertDoesNotThrow(() -> factory.newDocumentBuilder().parse(
                new org.xml.sax.InputSource(new java.io.StringReader(response.body()))),
                "US228: exported KML must be well-formed XML, including escaped route names");
        assertEquals("http://www.opengis.net/kml/2.2", document.getDocumentElement().getNamespaceURI());
        assertEquals("kml", document.getDocumentElement().getLocalName());
        var names = document.getElementsByTagNameNS("http://www.opengis.net/kml/2.2", "name");
        boolean found = false;
        for (int i = 0; i < names.getLength(); i++)
            found |= route.path("routeName").asText().equals(names.item(i).getTextContent());
        assertTrue(found, "Created route must be exported");
    }

}
