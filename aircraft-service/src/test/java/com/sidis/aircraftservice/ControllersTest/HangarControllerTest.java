package com.sidis.aircraftservice.ControllersTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.*;
import com.sidis.aircraftservice.Aircraft.domain.*;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.infrastructure.client.FlightRestClientService;
import com.sidis.aircraftservice.infrastructure.client.PeerForwarder;
import com.sidis.aircraftservice.infrastructure.client.RouteClientDTO;
import com.sidis.aircraftservice.infrastructure.client.RouteRestClientService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(HangarController.class)
@AutoConfigureMockMvc
@Import(HangarControllerTest.MethodSecurityTestConfig.class)
class HangarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddAircraftUseCase addAircraftUseCase;

    @MockBean
    private AircraftModelSearchService aircraftModelSearchService;

    @MockBean
    private AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;

    @MockBean
    private AircraftSearchService aircraftSearchService;

    @MockBean
    private RouteRestClientService routeClient;

    @MockBean
    private RouteSearchService routeSearchService;

    @MockBean
    private FlightRestClientService flightClient;

    @MockBean
    private FuelEfficiencyService fuelEfficiencyService;

    @MockBean
    private PeerForwarder peerForwarder;

    private AircraftRedis aircraft;

    @BeforeEach
    void setUp() {
        aircraft = mock(AircraftRedis.class);
        when(aircraft.getRegistrationNumber()).thenReturn("CS-TAA");
        when(aircraft.getModelName()).thenReturn("Airbus A320");
        when(aircraft.getStatus()).thenReturn(AircraftAvailability.values()[0]);
        when(aircraft.getManufacturingDate()).thenReturn(LocalDate.of(2020, 1, 1));
        when(aircraft.getTotalOperationalHours()).thenReturn(1500.0);
        when(aircraft.getTotalFlightHours()).thenReturn(1200.0);
        when(aircraft.getMeanRange()).thenReturn(5000.0);
        when(aircraft.getFeatures()).thenReturn(List.of("GPS", "RADAR"));
    }

    // POST /api/hangar/aircraft
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldAddAircraftSuccessfully() throws Exception {
        String availability = AircraftAvailability.values()[0].name();
        String json = """
                {
                    "registrationNumber": "CS-TAA",
                    "modelName": "Airbus A320",
                    "manufacturingDate": "2020-01-01",
                    "totalOperationalHours": 1500.0,
                    "totalFlightHours": 1200.0,
                    "availability": "%s"
                }
                """.formatted(availability);
        mockMvc.perform(post("/api/hangar/aircraft")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isCreated());
        verify(aircraftLifeCycleUpdaterService)
                .changeAvailability("CS-TAA", AircraftAvailability.valueOf(availability));
        verify(addAircraftUseCase).execute(
                eq("CS-TAA"),
                eq("Airbus A320"),
                eq(LocalDate.of(2020, 1, 1)),
                eq(1500.0),
                eq(1200.0)
        );
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn403WhenBackofficeTriesToAddAircraft() throws Exception {
        String availability = AircraftAvailability.values()[0].name();
        String json = """
                {
                    "registrationNumber": "CS-TAA",
                    "modelName": "Airbus A320",
                    "manufacturingDate": "2020-01-01",
                    "totalOperationalHours": 1500.0,
                    "totalFlightHours": 1200.0,
                    "availability": "%s"
                }
                """.formatted(availability);
        mockMvc.perform(post("/api/hangar/aircraft")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden());
        verifyNoInteractions(addAircraftUseCase);
    }

    // GET /api/hangar/aircraft/{registrationNumber}
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetAircraftSuccessfully() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("CS-TAA"))
                .thenReturn(Optional.of(aircraft));
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("CS-TAA"))
                .andExpect(jsonPath("$.modelName").value("Airbus A320"))
                .andExpect(jsonPath("$.totalOperationalHours").value(1500.0));
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetAircraftWhenUserIsBackoffice() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("CS-TAA"))
                .thenReturn(Optional.of(aircraft));
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserHasNoPermissionToGetAircraft() throws Exception {
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/aircraft/{registrationNumber}/status
    @Test
    @WithMockUser
    void shouldGetAircraftStatusSuccessfully() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("CS-TAA"))
                .thenReturn(Optional.of(aircraft));
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(AircraftAvailability.values()[0].name()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenAircraftStatusDoesNotExist() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("UNKNOWN"))
                .thenReturn(Optional.empty());
        mockMvc.perform(get("/api/hangar/aircraft/UNKNOWN/status"))
                .andExpect(status().isNotFound());
    }

    // PATCH /api/hangar/aircraft/{registrationNumber}
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldChangeAircraftAvailabilitySuccessfully() throws Exception {
        String availability = AircraftAvailability.values()[0].name();
        String json = """
                {
                    "registrationNumber": "CS-TAA",
                    "availability": "%s"
                }
                """.formatted(availability);
        mockMvc.perform(patch("/api/hangar/aircraft/CS-TAA")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk());
        verify(aircraftLifeCycleUpdaterService)
                .changeAvailability("CS-TAA", AircraftAvailability.valueOf(availability));
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToChangeAircraft() throws Exception {
        String availability = AircraftAvailability.values()[0].name();
        String json = """
                {
                    "registrationNumber": "CS-TAA",
                    "availability": "%s"
                }
                """.formatted(availability);
        mockMvc.perform(patch("/api/hangar/aircraft/CS-TAA")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden());
        verify(aircraftLifeCycleUpdaterService, never())
                .changeAvailability(anyString(), any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn409WhenRegistrationNumbersDoNotMatch() throws Exception {
        String availability = AircraftAvailability.values()[0].name();
        String json = """
                {
                    "registrationNumber": "CS-TBB",
                    "availability": "%s"
                }
                """.formatted(availability);
        mockMvc.perform(patch("/api/hangar/aircraft/CS-TAA")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isConflict());
        verify(aircraftLifeCycleUpdaterService, never())
                .changeAvailability(anyString(), any());
    }

    // GET /api/hangar/aircraft (filtering)
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetAircraftsWithFiltering() throws Exception {
        when(aircraftSearchService.findAircraftsMatchingFilter(
                isNull(), isNull(), isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(aircraft)));
        mockMvc.perform(get("/api/hangar/aircraft")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("CS-TAA"))
                .andExpect(jsonPath("$[0].modelName").value("Airbus A320"));
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn403WhenBackofficeTriesToFilterAircrafts() throws Exception {
        mockMvc.perform(get("/api/hangar/aircraft"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/aircraft/{registrationNumber}/compatible-routes
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetCompatibleRoutesSuccessfully() throws Exception {
        List<RouteClientDTO> routes = List.of();
        when(aircraftSearchService.spotAircraftInHangar("CS-TAA"))
                .thenReturn(Optional.of(aircraft));
        when(routeClient.getAllRoutes()).thenReturn(routes);
        when(routeSearchService.findCompatibleRoutes(aircraft, routes))
                .thenReturn(routes);
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA/compatible-routes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(routeClient).getAllRoutes();
        verify(routeSearchService).findCompatibleRoutes(aircraft, routes);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn403WhenBackofficeTriesToGetCompatibleRoutes() throws Exception {
        mockMvc.perform(get("/api/hangar/aircraft/CS-TAA/compatible-routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn404WhenAircraftForCompatibleRoutesDoesNotExist() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("UNKNOWN"))
                .thenReturn(Optional.empty());
        mockMvc.perform(get("/api/hangar/aircraft/UNKNOWN/compatible-routes"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(routeClient);
    }

    // GET /api/hangar/analytics/aircraft-availability
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetAircraftsGroupedByAvailability() throws Exception {
        when(aircraftSearchService.findAll())
                .thenReturn(List.of(aircraft));
        when(aircraftSearchService.findAircraftsByAvailability(
                anyList(), any(AircraftAvailability.class)))
                .thenReturn(List.of(aircraft));
        mockMvc.perform(get("/api/hangar/analytics/aircraft-availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
        verify(aircraftSearchService).findAll();
        verify(aircraftSearchService, times(AircraftAvailability.values().length))
                .findAircraftsByAvailability(anyList(), any(AircraftAvailability.class));
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn403WhenBackofficeTriesToGetAircraftAvailability() throws Exception {
        mockMvc.perform(get("/api/hangar/analytics/aircraft-availability"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/analytics/status-summary
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetAircraftAvailabilitySummary() throws Exception {
        when(aircraftSearchService.countByAvailability(any(AircraftAvailability.class)))
                .thenReturn(5L);
        mockMvc.perform(get("/api/hangar/analytics/status-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
        verify(aircraftSearchService, times(AircraftAvailability.values().length))
                .countByAvailability(any(AircraftAvailability.class));
    }

    // GET /api/hangar/analytics/fuel-efficiency
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetFuelEfficiency() throws Exception {
        when(aircraftSearchService.findAll()).thenReturn(List.of());
        when(flightClient.getAllFlights()).thenReturn(List.of());
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToGetFuelEfficiency() throws Exception {
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/analytics/operational-hours
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetOperationalHours() throws Exception {
        when(aircraftSearchService.findPages(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(aircraft)));
        mockMvc.perform(get("/api/hangar/analytics/operational-hours")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CS-TAA").value(1500.0));
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToGetOperationalHours() throws Exception {
        mockMvc.perform(get("/api/hangar/analytics/operational-hours"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/analytics/fuel-efficiency/aircraft
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetFuelEfficiencyPerAircraft() throws Exception {
        when(fuelEfficiencyService.getFuelEfficiencyPerAircraft())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/aircraft"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(fuelEfficiencyService).getFuelEfficiencyPerAircraft();
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToGetFuelEfficiencyPerAircraft() throws Exception {
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/aircraft"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/analytics/fuel-efficiency/routes
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetFuelEfficiencyPerRoute() throws Exception {
        when(fuelEfficiencyService.getFuelEfficiencyPerRoute())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/routes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(fuelEfficiencyService).getFuelEfficiencyPerRoute();
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToGetFuelEfficiencyPerRoute() throws Exception {
        mockMvc.perform(get("/api/hangar/analytics/fuel-efficiency/routes"))
                .andExpect(status().isForbidden());
    }

    // GET /api/hangar/aircraft-features
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetAircraftsWithFeatures() throws Exception {
        when(aircraftSearchService.findAircraftWithFeature(List.of("GPS")))
                .thenReturn(List.of(aircraft));
        String json = """
                {
                    "features": ["GPS"]
                }
                """;
        mockMvc.perform(get("/api/hangar/aircraft-features")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("CS-TAA"));
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenAtccTriesToSearchAircraftByFeatures() throws Exception {
        String json = """
                {
                    "features": ["GPS"]
                }
                """;
        mockMvc.perform(get("/api/hangar/aircraft-features")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenFeatureListIsEmpty() throws Exception {
        String json = """
                {
                    "features": []
                }
                """;
        mockMvc.perform(get("/api/hangar/aircraft-features")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
