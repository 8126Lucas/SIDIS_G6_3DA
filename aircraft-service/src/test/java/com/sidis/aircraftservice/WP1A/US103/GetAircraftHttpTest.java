package com.sidis.aircraftservice.WP1A.US103;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HangarController.class)
@AutoConfigureMockMvc(addFilters = false)
class GetAircraftHttpTest {

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
    void shouldReturnAircraftWhenExists() throws Exception {

        AircraftModel model = mock(AircraftModel.class);
        when(model.getModelName()).thenReturn("A320");

        Aircraft aircraft = mock(Aircraft.class);

        when(aircraft.getRegistrationNumber()).thenReturn("CS-TST");
        when(aircraft.getModel()).thenReturn(model);
        when(aircraft.getStatus()).thenReturn(AircraftAvailability.AVAILABLE);
        when(aircraft.getManufacturingDate()).thenReturn(LocalDate.of(2020, 1, 1));
        when(aircraft.getTotalOperationalHours()).thenReturn(1000.0);
        when(aircraft.getTotalFlightHours()).thenReturn(800.0);
        when(aircraft.getMeanRange()).thenReturn(5000.0);
        when(aircraft.getFeatures()).thenReturn(List.of("WiFi", "Extra Legroom"));

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.of(aircraft));

        mockMvc.perform(get("/api/hangar/aircraft/CS-TST")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("CS-TST"))
                .andExpect(jsonPath("$.modelName").value("A320"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void shouldReturn404WhenAircraftNotFound() throws Exception {

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/hangar/aircraft/CS-TST"))
                .andExpect(status().isNotFound());
    }
}