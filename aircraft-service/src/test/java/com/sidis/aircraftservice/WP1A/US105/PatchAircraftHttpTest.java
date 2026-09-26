package com.sidis.aircraftservice.WP1A.US105;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HangarController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatchAircraftHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddAircraftUseCase addAircraftUseCase;

    @MockBean
    private AircraftSearchService aircraftSearchService;

    @MockBean
    private AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;

    @MockBean
    private AircraftModelSearchService aircraftModelSearchService;

/*    @MockBean
    private RouteSearchService routeSearchService;

    @MockBean
    private FlightSearchService flightSearchService;

    @MockBean
    private FuelEfficiencyService fuelEfficiencyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;*/

    @Test
    void shouldReturn200WhenAvailabilityChangesSuccessfully() throws Exception {

        Aircraft aircraft = mock(Aircraft.class);

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.of(aircraft));

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(patch("/api/hangar/aircraft/CS-TST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(aircraftSearchService, times(1))
                .spotAircraftInHangar("CS-TST");

        verify(aircraftLifeCycleUpdaterService, times(1))
                .changeAvailability(
                        eq(aircraft),
                        eq(AircraftAvailability.AVAILABLE)
                );
    }

    @Test
    void shouldReturn404WhenAircraftDoesNotExist() throws Exception {

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.empty());

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(patch("/api/hangar/aircraft/CS-TST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn500WhenServiceFails() throws Exception {

        Aircraft aircraft = mock(Aircraft.class);

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.of(aircraft));

        doThrow(new RuntimeException("Database failure"))
                .when(aircraftLifeCycleUpdaterService)
                .changeAvailability(any(), any());

        String json = "{\n" +
                      "    \"registrationNumber\": \"CS-TST\",\n" +
                      "    \"availability\": \"AVAILABLE\"\n" +
                      "}\n";

        mockMvc.perform(patch("/api/hangar/aircraft/CS-TST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }
}