package com.sidis.aircraftservice.WP1A.US102;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Route.application.RouteSearchService;
//import com.sidis.aircraftservice.authUsers.application.JwtService;
//import com.sidis.aircraftservice.authUsers.infrastructure.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HangarController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddAircraftHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddAircraftUseCase addAircraftUseCase;

    @MockBean
    private AircraftModelSearchService aircraftModelSearchService;

    @MockBean
    private AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;

    @MockBean
    private AircraftSearchService aircraftSearchService;

/*    @MockBean
    private RouteSearchService routeSearchService;

    @MockBean
    private FlightSearchService flightSearchService;

    @MockBean
    private FuelEfficiencyService fuelEfficiencyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
*/
    @Test
    void shouldCreateAircraftAndValidateRequestMapping() throws Exception {

        AircraftModel model = mock(AircraftModel.class);
        when(model.getModelName()).thenReturn("Airbus A320");

        when(aircraftModelSearchService.spotAircraftInCatalog("Airbus A320"))
                .thenReturn(Optional.of(model));

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"modelName\": \"Airbus A320\",\n" +
                      "    \"manufacturingDate\": \"2020-01-01\",\n" +
                      "    \"totalOperationalHours\": 1000.0,\n" +
                      "    \"totalFlightHours\": 800.0,\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(post("/api/hangar/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(aircraftModelSearchService, times(1))
                .spotAircraftInCatalog("Airbus A320");

        verify(aircraftLifeCycleUpdaterService, times(1))
                .changeAvailability(any(), eq(
                        com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability.AVAILABLE
                ));

        verify(addAircraftUseCase, times(1))
                .execute(any());
    }

    @Test
    void shouldReturn404WhenModelNotFound() throws Exception {

        when(aircraftModelSearchService.spotAircraftInCatalog("Airbus A320"))
                .thenReturn(Optional.empty());

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"modelName\": \"Airbus A320\",\n" +
                      "    \"manufacturingDate\": \"2020-01-01\",\n" +
                      "    \"totalOperationalHours\": 1000.0,\n" +
                      "    \"totalFlightHours\": 800.0,\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(post("/api/hangar/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn500WhenServiceFails() throws Exception {

        when(aircraftModelSearchService.spotAircraftInCatalog(any()))
                .thenThrow(new RuntimeException("DB failure"));

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"modelName\": \"Airbus A320\",\n" +
                      "    \"manufacturingDate\": \"2020-01-01\",\n" +
                      "    \"totalOperationalHours\": 1000.0,\n" +
                      "    \"totalFlightHours\": 800.0,\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(post("/api/hangar/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }
}