package com.sidis.aircraftservice.WP1B.US204;

import com.sidis.aircraftservice.support.OperationalHttpTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetTopFiveModelsHttpTest extends OperationalHttpTest {
    @Test
    void shouldRankModelsByNumberOfAssignments() throws Exception {
        List<String> aircrafts=seedAircrafts();
        assertNotNull(aircrafts);
        assertTrue(aircrafts.size() >= 2);
        assertEquals(8, aircrafts.size());

        var firstAircraft=get("/api/hangar/aircraft/"+aircrafts.getFirst());
        var lastAircraft=get("/api/hangar/aircraft/"+aircrafts.getLast());

        seedFlights(aircrafts.getFirst(),30);
        seedFlights(aircrafts.getLast(),60);

        String modelFirst = firstAircraft.path("modelName").asText();
        String modelLast = lastAircraft.path("modelName").asText();
        var response = get("/api/catalog/rankings?mode=assignments");

        assertTrue(response.isArray());

        int indexFirst = indexOfModel(response, modelFirst);
        int indexLast = indexOfModel(response, modelLast);

        assertTrue(indexFirst >= 0, "Model A não aparece no top 5");
        assertTrue(indexLast >= 0, "Model B não aparece no top 5");

        assertTrue(indexLast < indexFirst,"O modelo com 6 atribuições deveria aparecer antes do modelo com 3");
    }

    @Test
    void shouldRankModelsByFlightHours() throws Exception {
        String aircraft1=createAircraft(100000.0);
        String aircraft2=createAircraft(653300000.0);

        var firstAircraft = get("/api/hangar/aircraft/" + aircraft1);
        var lastAircraft = get("/api/hangar/aircraft/" +aircraft2);

        String modelFirst = firstAircraft.path("modelName").asText();
        String modelLast = lastAircraft.path("modelName").asText();

        var response = get("/api/catalog/rankings?mode=flight-hours");

        assertTrue(response.isArray());

        int indexFirst = indexOfModel(response, modelFirst);
        int indexLast = indexOfModel(response, modelLast);

        assertTrue(indexFirst >= 0, "Model A não aparece no top 5");
        assertTrue(indexLast >= 0, "Model B não aparece no top 5");

        assertTrue(indexLast < indexFirst,"O modelo com mais horas de voo deveria aparecer antes do modelo com menos horas");
    }

    @Test
    void shouldReturnEmptyListWhenModeIsInvalid() throws Exception {
        var response = get("/api/catalog/rankings?mode=");
        assertTrue(response.isArray(), response.toString());
        assertEquals(0, response.size());
    }

    @Disabled("A real gateway request cannot inject a database failure.")
    @Test
    void shouldReturn500WhenServiceFails() {}
}
