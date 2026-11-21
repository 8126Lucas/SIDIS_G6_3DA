package com.sidis.routeservice.ControllerTest;

import com.sidis.routeservice.Route.application.RouteExportService;
import com.sidis.routeservice.Route.application.RouteQueryService;
import com.sidis.routeservice.Route.application.RouteService;
import com.sidis.routeservice.Route.domain.AirportReference;
import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.domain.RouteHistory;
import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.domain.RouteRequirements;
import com.sidis.routeservice.Route.domain.RouteStatus;
import com.sidis.routeservice.Route.domain.RouteType;
import com.sidis.routeservice.RouteController;
import com.sidis.routeservice.exceptions.DomainException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc
class RouteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteService routeService;

    @MockBean
    private RouteQueryService routeQueryService;

    @MockBean
    private RouteExportService routeExportService;

    private Route route;

    @BeforeEach
    void setUp() {
        AirportReference origin = new AirportReference("OPO", null);
        AirportReference destination = new AirportReference("LIS", null);
        RouteRequirements requirements = mock(RouteRequirements.class);
        RouteHistory history = mock(RouteHistory.class);
        route = new Route(requirements, RouteStatus.ACTIVE, RouteType.DIRECT,
                history, 1.5, 280.0,
                origin, destination, "Porto-Lisboa");
    }

    // POST /api/routes
    @Test
    @WithMockUser
    void shouldCreateRouteSuccessfully() throws Exception {
        when(routeService.createRoute(anyString(), anyString(), anyString(), anyDouble(), any(), any(RouteRequirements.class))).thenReturn(route);
        mockMvc.perform(post("/api/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRouteJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeName")
                        .value("Porto-Lisboa"))
                .andExpect(jsonPath("$.originIataCode")
                        .value("OPO"))
                .andExpect(jsonPath("$.destinationIataCode")
                        .value("LIS"));
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenRequirementsAreNull() throws Exception {
        String json = """
                {
                    "routeName": "Porto-Lisboa",
                    "originIataCode": "OPO",
                    "destinationIataCode": "LIS",
                    "estimatedFlightTimeHours": 1.5,
                    "type": "DIRECT",
                    "requirements": null
                }
                """;
        mockMvc.perform(post("/api/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
        verifyNoInteractions(routeService);
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenCreateRouteThrowsDomainException() throws Exception {
        when(routeService.createRoute(
                anyString(),
                anyString(),
                anyString(),
                anyDouble(),
                any(),
                any(RouteRequirements.class)
        )).thenThrow(new DomainException("Route already exists"));
        mockMvc.perform(post("/api/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRouteJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void shouldReturn500WhenCreateRouteThrowsUnexpectedException() throws Exception {
        when(routeService.createRoute(
                anyString(),
                anyString(),
                anyString(),
                anyDouble(),
                any(),
                any(RouteRequirements.class)
        )).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRouteJson()))
                .andExpect(status().isInternalServerError());
    }

    // PATCH /api/routes/{routeId}
    @Test
    @WithMockUser
    void shouldUpdateRouteSuccessfully() throws Exception {
        when(routeService.updateRoute(
                eq(1L),
                eq("FAO"),
                eq(2.0),
                isNull(),
                isNull()
        )).thenReturn(route);
        mockMvc.perform(patch("/api/routes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "destinationIataCode": "FAO",
                                "estimatedFlightTimeHours": 2.0,
                                "status": null,
                                "history": null
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeName")
                        .value("Porto-Lisboa"));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenUpdateJsonIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/routes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // GET /api/routes/{routeId}/history
    @Test
    @WithMockUser
    void shouldGetRouteHistorySuccessfully() throws Exception {
        RouteReadModel.HistoryData history = mock(RouteReadModel.HistoryData.class);
        when(history.routeUsage()).thenReturn(10);
        when(routeQueryService.getRouteHistory(1L)).thenReturn(history);
        mockMvc.perform(get("/api/routes/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeUsage").value(10));
    }

    // GET /api/routes/from/{airportIataCode}
    @Test
    @WithMockUser
    void shouldGetRoutesFromAirportSuccessfully() throws Exception {
        when(routeQueryService.getRoutesFromAirport("OPO")).thenReturn(List.of());
        mockMvc.perform(get("/api/routes/from/OPO"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenAirportIataCodeIsInvalid() throws Exception {
        when(routeQueryService.getRoutesFromAirport("porto"))
                .thenThrow(new DomainException(
                        "IATA code must have 3 uppercase letters"));
        mockMvc.perform(get("/api/routes/from/porto"))
                .andExpect(status().isConflict());
    }

    // GET /api/routes/{routeId}
    @Test
    @WithMockUser
    void shouldGetRouteDetailsSuccessfully() throws Exception {
        RouteReadModel model = readModel();
        when(routeQueryService.getRouteDetails(1L))
                .thenReturn(model);
        mockMvc.perform(get("/api/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(1))
                .andExpect(jsonPath("$.routeName")
                        .value("Porto-Lisboa"));
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenRouteDoesNotExist() throws Exception {
        when(routeQueryService.getRouteDetails(999L))
                .thenThrow(new DomainException("Route does not exist"));
        mockMvc.perform(get("/api/routes/999"))
                .andExpect(status().isConflict());
    }

    // GET /api/routes
    @Test
    @WithMockUser
    void shouldSearchAllRoutesSuccessfully() throws Exception {
        when(routeQueryService.searchRoutes(null, null)).thenReturn(List.of());
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldSearchRoutesByOriginAndDestination() throws Exception {
        when(routeQueryService.searchRoutes("OPO", "LIS")).thenReturn(List.of());
        mockMvc.perform(get("/api/routes")
                        .param("origin", "OPO")
                        .param("destination", "LIS"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenSearchUsesInvalidIataCode() throws Exception {
        when(routeQueryService.searchRoutes("porto", null))
                .thenThrow(new DomainException(
                        "IATA code must have 3 uppercase letters"));
        mockMvc.perform(get("/api/routes")
                        .param("origin", "porto"))
                .andExpect(status().isConflict());
    }

    // GET /api/routes/active
    @Test
    @WithMockUser
    void shouldGetActiveRoutesSuccessfully() throws Exception {
        when(routeQueryService.listActiveRoutes("popularity"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/routes/active"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldGetActiveRoutesSortedByDistance() throws Exception {
        when(routeQueryService.listActiveRoutes("distance"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/routes/active")
                        .param("sortBy", "distance"))
                .andExpect(status().isOk());
    }

    // GET /api/routes/total-distance
    @Test
    @WithMockUser
    void shouldCalculateTotalDistanceSuccessfully() throws Exception {
        when(routeQueryService.calculateTotalRouteDistance())
                .thenReturn(1000.0);
        mockMvc.perform(get("/api/routes/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceKm")
                        .value(1000.0));
    }

    // GET /api/routes/export/geojson
    @Test
    @WithMockUser
    void shouldExportGeoJsonSuccessfully() throws Exception {
        String geoJson = """
                {
                    "type": "FeatureCollection",
                    "features": []
                }
                """;
        when(routeExportService.exportAsGeoJson())
                .thenReturn(geoJson);
        mockMvc.perform(get("/api/routes/export/geojson"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.valueOf("application/geo+json")))
                .andExpect(content().string(geoJson));
    }

    @Test
    @WithMockUser
    void shouldReturn500WhenGeoJsonExportFails() throws Exception {
        when(routeExportService.exportAsGeoJson())
                .thenThrow(new RuntimeException("Export failed"));
        mockMvc.perform(get("/api/routes/export/geojson"))
                .andExpect(status().isInternalServerError());
    }

    // GET /api/routes/export/kml
    @Test
    @WithMockUser
    void shouldExportKmlSuccessfully() throws Exception {
        String kml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <kml>
                    <Document>
                        <name>Route Network</name>
                    </Document>
                </kml>
                """;
        when(routeExportService.exportAsKml()).thenReturn(kml);
        mockMvc.perform(get("/api/routes/export/kml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.valueOf(
                                "application/vnd.google-earth.kml+xml")))
                .andExpect(content().string(kml));
    }

    @Test
    @WithMockUser
    void shouldReturn500WhenKmlExportFails() throws Exception {
        when(routeExportService.exportAsKml())
                .thenThrow(new RuntimeException("Export failed"));
        mockMvc.perform(get("/api/routes/export/kml"))
                .andExpect(status().isInternalServerError());
    }

    // GET /api/routes/alternatives
    @Test
    @WithMockUser
    void shouldGetAlternativeRoutesSuccessfully() throws Exception {
        when(routeQueryService.searchAlternativeRoutes("OPO", "LIS"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/routes/alternatives")
                        .param("origin", "OPO")
                        .param("destination", "LIS"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldReturn500WhenAlternativeRouteParametersAreMissing() throws Exception {
        mockMvc.perform(get("/api/routes/alternatives")
                        .param("origin", "OPO"))
                .andExpect(status().isInternalServerError());
    }

    private String validCreateRouteJson() {
        return """
                {
                    "routeName": "Porto-Lisboa",
                    "originIataCode": "OPO",
                    "destinationIataCode": "LIS",
                    "estimatedFlightTimeHours": 1.5,
                    "type": "DIRECT",
                    "requirements": {
                        "requiredRange": 300.0,
                        "requiredCapacity": 100
                    }
                }
                """;
    }

    private RouteReadModel readModel() {
        RouteReadModel model = mock(RouteReadModel.class);

        when(model.routeId()).thenReturn(1L);
        when(model.routeName()).thenReturn("Porto-Lisboa");
        when(model.status()).thenReturn(RouteStatus.ACTIVE);
        when(model.type()).thenReturn(RouteType.DIRECT);
        when(model.originAirport()).thenReturn(null);
        when(model.destinationAirport()).thenReturn(null);
        when(model.estimatedFlightTimeHours()).thenReturn(1.5);
        when(model.distanceKm()).thenReturn(280.0);
        when(model.routeRequirements()).thenReturn(null);
        when(model.routeHistory()).thenReturn(null);

        return model;
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}