package com.sidis.airportservice.US225;

import com.sidis.airportservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** US225 acceptance tests over the real Gateway. Generated with OpenAI Codex. */
class ImportAirportsCsvHttpTest extends OperationalHttpTest {

    @Test void shouldImportMultipleAirportsFromCsv() throws Exception {
        String first = unusedIata();
        String second;
        do { second = unusedIata(); } while (first.equals(second));
        String csv = "iataCode,type,name,status,city,longitude,latitude,region,timezone,country,routeDistance,runways,certifications,contacts,images\n"
                + row(first) + row(second);
        expect(200, postCsv("/api/airports/import", csv));
        for (String code : List.of(first, second)) {
            var airport = get("/api/airports/" + code);
            assertEquals(code, airport.path("iataCode").asText());
            assertEquals("Imported-" + code, airport.path("name").asText());
            assertEquals("Porto", airport.path("location").path("city").asText());
            assertEquals("03/21", airport.path("runways").get(0).path("runwayName").asText());
            assertEquals("ops@example.com", airport.path("contacts").get(0).path("value").asText());
        }
    }

    private String row(String code) {
        return code + ",International,Imported-" + code
                + ",OPERATIONAL,Porto,-8.68,41.24,Norte,Europe/Lisbon,Portugal,10,03/21|3000|NE/SW|OPEN,,EMAIL|ops@example.com,https://example.com/photo.jpg\n";
    }

}
