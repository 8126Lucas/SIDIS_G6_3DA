package com.sidis.aircraftservice.US227;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HangarController.class)
@AutoConfigureMockMvc(addFilters = false)
class FuelEfficiencyHttpTest {

    @Autowired
    private MockMvc mockMvc;

//    @MockBean private FuelEfficiencyService fuelEfficiencyService;
    @MockBean private AddAircraftUseCase addAircraftUseCase;
    @MockBean private AircraftModelSearchService aircraftModelSearchService;
    @MockBean private AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;
    @MockBean private AircraftSearchService aircraftSearchService;
//    @MockBean private RouteSearchService routeSearchService;
//    @MockBean private FlightSearchService flightSearchService;
//    @MockBean private JwtService jwtService;
//    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

/*    @Test
    void shouldReturnFuelEfficiencyPerAircraft() throws Exception {
        FuelEfficiencyService.AircraftFuelEfficiency entry =
                new FuelEfficiencyService.AircraftFuelEfficiency(
                        "CS-TVA", "Boeing 737", 20000.0, 5000.0, 4.0);

        when(fuelEfficiencyService.getFuelEfficiencyPerAircraft()).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/aircraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("CS-TVA"))
                .andExpect(jsonPath("$[0].modelName").value("Boeing 737"))
                .andExpect(jsonPath("$[0].fuelCapacityLiters").value(20000.0))
                .andExpect(jsonPath("$[0].maxRangeKm").value(5000.0))
                .andExpect(jsonPath("$[0].fuelConsumptionLitPerKm").value(4.0));

        verify(fuelEfficiencyService, times(1)).getFuelEfficiencyPerAircraft();
    }*/

/*    @Test
    void shouldReturnEmptyListWhenNoAircraft() throws Exception {
        when(fuelEfficiencyService.getFuelEfficiencyPerAircraft()).thenReturn(List.of());

        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/aircraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }*/

/*    @Test
    void shouldReturnFuelEfficiencyPerRoute() throws Exception {
        FuelEfficiencyService.RouteFuelEfficiency route =
                new FuelEfficiencyService.RouteFuelEfficiency(
                        1L, "LIS-OPO", "LIS", "OPO", 300.0, 4.0, 1200.0);

        when(fuelEfficiencyService.getFuelEfficiencyPerRoute()).thenReturn(List.of(route));

        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(1))
                .andExpect(jsonPath("$[0].routeName").value("LIS-OPO"))
                .andExpect(jsonPath("$[0].originIata").value("LIS"))
                .andExpect(jsonPath("$[0].destinationIata").value("OPO"))
                .andExpect(jsonPath("$[0].distanceKm").value(300.0))
                .andExpect(jsonPath("$[0].avgFuelConsumptionLitPerKm").value(4.0))
                .andExpect(jsonPath("$[0].estimatedFuelLiters").value(1200.0));

        verify(fuelEfficiencyService, times(1)).getFuelEfficiencyPerRoute();
    }*/

/*    @Test
    void shouldReturnEmptyListWhenNoFlights() throws Exception {
        when(fuelEfficiencyService.getFuelEfficiencyPerRoute()).thenReturn(List.of());

        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }*/
}
