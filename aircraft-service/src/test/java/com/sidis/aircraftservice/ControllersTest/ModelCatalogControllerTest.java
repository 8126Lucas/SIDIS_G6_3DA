
package com.sidis.aircraftservice.ControllersTest;

import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.AircraftCatalog.ModelCatalogController;
import com.sidis.aircraftservice.AircraftCatalog.application.AddModelUseCase;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelUpdater;
import com.sidis.aircraftservice.AircraftCatalog.application.FlightSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecsRedis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelCatalogController.class)
@AutoConfigureMockMvc
@Import(ModelCatalogControllerTest.MethodSecurityTestConfig.class)
class ModelCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AircraftModelSearchService aircraftModelSearchService;

    @MockBean
    private FlightSearchService flightSearchService;

    @MockBean
    private AircraftSearchService aircraftSearchService;

    @MockBean
    private AddModelUseCase addModelUseCase;

    @MockBean
    private AircraftModelUpdater aircraftModelUpdater;

    private AircraftModelRedis model;
    private AircraftSpecsRedis specs;

    @BeforeEach
    void setUp() {
        model = mock(AircraftModelRedis.class);
        specs = mock(AircraftSpecsRedis.class);
        given(model.getModelName()).willReturn("A320");
        given(model.getManufacturer()).willReturn("Airbus");
        given(model.getModelImage()).willReturn(List.of("image1.png"));
        given(model.getAircraftModelSpecs()).willReturn(specs);
        given(specs.getFuelCapacity()).willReturn(24210.0);
        given(specs.getMaximumRange()).willReturn(6100.0);
        given(specs.getCruisingSpeed()).willReturn(840.0);
        given(specs.getStandardSeatingCapacity()).willReturn(180);
    }

    // ==================================================
    // GET /api/catalog/{modelName}
    // ==================================================

    @Test
    @WithMockUser
    void shouldReturn200AndModelJsonWhenModelExists() throws Exception {
        given(aircraftModelSearchService.spotAircraftInCatalog("A320"))
                .willReturn(Optional.of(model));
        mockMvc.perform(get("/api/catalog/A320"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.modelName").value("A320"))
                .andExpect(jsonPath("$.manufacturer").value("Airbus"))
                .andExpect(jsonPath("$.modelImage[0]").value("image1.png"))
                .andExpect(jsonPath("$.specs.fuelCapacityLiters").value(24210.0))
                .andExpect(jsonPath("$.specs.maximumRangeKm").value(6100.0))
                .andExpect(jsonPath("$.specs.cruisingSpeedKph").value(840.0))
                .andExpect(jsonPath("$.specs.standardSeatingCapacity").value(180));
        verify(aircraftModelSearchService).spotAircraftInCatalog("A320");
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenModelDoesNotExist() throws Exception {
        given(aircraftModelSearchService.spotAircraftInCatalog("UNKNOWN"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/UNKNOWN"))
                .andExpect(status().isNotFound());

        verify(aircraftModelSearchService).spotAircraftInCatalog("UNKNOWN");
    }

    @Test
    @WithMockUser
    void shouldReturn404ForAnotherUnknownModel() throws Exception {
        given(aircraftModelSearchService.spotAircraftInCatalog("B999"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/B999"))
                .andExpect(status().isNotFound());
    }

    // ==================================================
    // POST /api/catalog/model
    // ==================================================

    private String validCreateRequest() {
        return """
                {
                  "modelName": "A320",
                  "modelImage": ["image1.png"],
                  "manufacturer": "Airbus",
                  "specs": {
                    "fuelCapacityLiters": 24210.0,
                    "maximumRangeKm": 6100.0,
                    "cruisingSpeedKph": 840.0,
                    "standardSeatingCapacity": 180
                  }
                }
                """;
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn201WhenModelIsCreated() throws Exception {
        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated());

        verify(addModelUseCase).execute(any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenModelNameIsBlank() throws Exception {
        String request = """
                {
                  "modelName": "",
                  "modelImage": ["image1.png"],
                  "manufacturer": "Airbus",
                  "specs": {
                    "fuelCapacityLiters": 24210.0,
                    "maximumRangeKm": 6100.0,
                    "cruisingSpeedKph": 840.0,
                    "standardSeatingCapacity": 180
                  }
                }
                """;

        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(addModelUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenManufacturerIsBlank() throws Exception {
        String request = """
                {
                  "modelName": "A320",
                  "modelImage": ["image1.png"],
                  "manufacturer": "",
                  "specs": {
                    "fuelCapacityLiters": 24210.0,
                    "maximumRangeKm": 6100.0,
                    "cruisingSpeedKph": 840.0,
                    "standardSeatingCapacity": 180
                  }
                }
                """;

        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(addModelUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenSpecsAreNull() throws Exception {
        String request = """
                {
                  "modelName": "A320",
                  "modelImage": ["image1.png"],
                  "manufacturer": "Airbus",
                  "specs": null
                }
                """;

        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(addModelUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        String request = """
                {
                  "modelName": "A320",
                  "manufacturer":
                """;

        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(addModelUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(addModelUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenUserIsNotBackoffice() throws Exception {
        mockMvc.perform(post("/api/catalog/model")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden());

        verify(addModelUseCase, never()).execute(any());
    }

    // ==================================================
    // PATCH /api/catalog/model/{modelName}
    // ==================================================

    private String validPatchRequest() {
        return """
                {
                  "modelName": "A320",
                  "modelImage": ["new-image.png"],
                  "manufacturer": "Airbus Updated",
                  "specs": {
                    "fuelCapacityLiters": 25000.0,
                    "maximumRangeKm": 6200.0,
                    "cruisingSpeedKph": 850.0,
                    "standardSeatingCapacity": 190
                  }
                }
                """;
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn200WhenModelIsUpdated() throws Exception {
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatchRequest()))
                .andExpect(status().isOk());
        verify(aircraftModelUpdater).aircraftModelUpdate(
                eq("A320"),
                eq(List.of("new-image.png")),
                eq("Airbus Updated"),
                eq(25000.0),
                eq(6200.0),
                eq(850.0),
                eq(190)
        );
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn409WhenPathAndBodyModelNamesDoNotMatch() throws Exception {
        String request = """
                {
                  "modelName": "B737",
                  "modelImage": ["new-image.png"],
                  "manufacturer": "Boeing",
                  "specs": null
                }
                """;
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
        verifyNoInteractions(aircraftModelUpdater);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn409WhenPatchContainsNoChanges() throws Exception {
        String request = """
                {
                  "modelName": "A320",
                  "modelImage": null,
                  "manufacturer": null,
                  "specs": null
                }
                """;
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
        verifyNoInteractions(aircraftModelUpdater);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenPatchModelNameIsBlank() throws Exception {
        String request = """
                {
                  "modelName": "",
                  "modelImage": ["new-image.png"],
                  "manufacturer": "Airbus",
                  "specs": null
                }
                """;
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aircraftModelUpdater);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenPatchJsonIsMalformed() throws Exception {
        String request = """
                {
                  "modelName": "A320",
                  "manufacturer":
                """;
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aircraftModelUpdater);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn400WhenPatchBodyIsMissing() throws Exception {
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aircraftModelUpdater);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenUnauthorizedUserTriesToUpdate() throws Exception {
        mockMvc.perform(patch("/api/catalog/model/A320")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatchRequest()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(aircraftModelUpdater);
    }

    // ==================================================
    // GET /api/catalog/rankings?mode=assignments
    // ==================================================

    @Test
    @WithMockUser
    void shouldReturn200WithAssignmentRanking() throws Exception {
        AircraftRedis aircraft1 = mock(AircraftRedis.class);
        AircraftRedis aircraft2 = mock(AircraftRedis.class);
        given(aircraft1.getModelName()).willReturn("A320");
        given(aircraft2.getModelName()).willReturn("B737");
        Map<AircraftRedis, Long> assignments = new HashMap<>();
        assignments.put(aircraft1, 10L);
        assignments.put(aircraft2, 5L);
        given(flightSearchService.findMostAssignedAircraftToFly())
                .willReturn(assignments);
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "assignments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].modelName").value("A320"))
                .andExpect(jsonPath("$[1].modelName").value("B737"));
        verify(flightSearchService).findMostAssignedAircraftToFly();
    }

    @Test
    @WithMockUser
    void shouldGroupAssignmentsForTheSameModel() throws Exception {
        AircraftRedis aircraft1 = mock(AircraftRedis.class);
        AircraftRedis aircraft2 = mock(AircraftRedis.class);
        AircraftRedis aircraft3 = mock(AircraftRedis.class);
        given(aircraft1.getModelName()).willReturn("A320");
        given(aircraft2.getModelName()).willReturn("A320");
        given(aircraft3.getModelName()).willReturn("B737");
        Map<AircraftRedis, Long> assignments = new HashMap<>();
        assignments.put(aircraft1, 10L);
        assignments.put(aircraft2, 5L);
        assignments.put(aircraft3, 8L);
        given(flightSearchService.findMostAssignedAircraftToFly())
                .willReturn(assignments);
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].modelName").value("A320"))
                .andExpect(jsonPath("$[1].modelName").value("B737"));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyAssignmentRankingWhenThereAreNoAssignments() throws Exception {
        given(flightSearchService.findMostAssignedAircraftToFly())
                .willReturn(new HashMap<>());
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "assignments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldLimitAssignmentRankingToFiveModels() throws Exception {
        Map<AircraftRedis, Long> assignments = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            AircraftRedis aircraft = mock(AircraftRedis.class);
            given(aircraft.getModelName()).willReturn("MODEL-" + i);
            assignments.put(aircraft, (long) i);
        }
        given(flightSearchService.findMostAssignedAircraftToFly())
                .willReturn(assignments);
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].modelName").value("MODEL-7"));
    }

    // ==================================================
    // GET /api/catalog/rankings?mode=flight-hours
    // ==================================================

    @Test
    @WithMockUser
    void shouldReturn200WithFlightHoursRanking() throws Exception {
        given(aircraftSearchService.findMostUsedModelsByFlyingAircraftHours())
                .willReturn(List.of(
                        new AbstractMap.SimpleEntry<>("A320", 1200.0),
                        new AbstractMap.SimpleEntry<>("B737", 800.0)
                ));
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "flight-hours"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].modelName").value("A320"))
                .andExpect(jsonPath("$[1].modelName").value("B737"));
        verify(aircraftSearchService).findMostUsedModelsByFlyingAircraftHours();
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyFlightHoursRankingWhenThereAreNoResults() throws Exception {
        given(aircraftSearchService.findMostUsedModelsByFlyingAircraftHours())
                .willReturn(List.of());
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "flight-hours"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser
    void shouldLimitFlightHoursRankingToFiveModels() throws Exception {
        given(aircraftSearchService.findMostUsedModelsByFlyingAircraftHours())
                .willReturn(List.of(
                        new AbstractMap.SimpleEntry<>("MODEL-1", 100.0),
                        new AbstractMap.SimpleEntry<>("MODEL-2", 200.0),
                        new AbstractMap.SimpleEntry<>("MODEL-3", 300.0),
                        new AbstractMap.SimpleEntry<>("MODEL-4", 400.0),
                        new AbstractMap.SimpleEntry<>("MODEL-5", 500.0),
                        new AbstractMap.SimpleEntry<>("MODEL-6", 600.0),
                        new AbstractMap.SimpleEntry<>("MODEL-7", 700.0)
                ));
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "flight-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].modelName").value("MODEL-7"));
    }

    // ==================================================
    // GET /api/catalog/rankings - invalid mode
    // ==================================================

    @Test
    @WithMockUser
    void shouldReturnEmptyListForUnknownRankingMode() throws Exception {
        mockMvc.perform(get("/api/catalog/rankings")
                        .param("mode", "unknown"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verifyNoInteractions(flightSearchService, aircraftSearchService);
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenRankingModeIsMissing() throws Exception {
        mockMvc.perform(get("/api/catalog/rankings"))
                .andExpect(status().isBadRequest());
    }

    // ==================================================
    // Test configuration
    // ==================================================

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
