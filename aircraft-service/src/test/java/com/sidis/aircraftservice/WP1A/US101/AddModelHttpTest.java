package com.sidis.aircraftservice.WP1A.US101;

import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.AircraftCatalog.ModelCatalogController;
import com.sidis.aircraftservice.AircraftCatalog.application.AddModelUseCase;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelUpdater;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.authUsers.application.JwtService;
//import com.sidis.aircraftservice.authUsers.infrastructure.JwtAuthenticationFilter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddModelHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddModelUseCase addModelUseCase;

    @MockBean
    private AircraftModelSearchService aircraftModelSearchService;

    @MockBean
    private AircraftModelUpdater aircraftModelUpdater;

//    @MockBean
//    private FlightSearchService flightSearchService;

    @MockBean
    private AircraftSearchService aircraftSearchService;

//    @MockBean
//    private JwtService jwtService;

//    @MockBean
//    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturn201WhenModelIsCreated() throws Exception {

        String json = "{\n" +
                      "    \"modelName\": \"Airbus A320\",\n" +
                      "    \"modelImage\": [\n" +
                      "        \"https://example.com/a320.jpg\"\n" +
                      "    ],\n" +
                      "    \"manufacturer\": \"Airbus\",\n" +
                      "    \"specs\": {\n" +
                      "        \"fuelCapacityLiters\": 24210.0,\n" +
                      "        \"maximumRangeKm\": 6150.0,\n" +
                      "        \"cruisingSpeedKph\": 828.0,\n" +
                      "        \"standardSeatingCapacity\": 180\n" +
                      "    }\n" +
                      "}\n";

        mockMvc.perform(post("/api/catalog/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(addModelUseCase, times(1))
                .execute(any());
    }

    @Test
    void shouldReturn400WhenModelNameIsBlank() throws Exception {

        String json = "{\n" +
                      "    \"modelName\": \"\",\n" +
                      "    \"modelImage\": [\n" +
                      "        \"https://example.com/a320.jpg\"\n" +
                      "    ],\n" +
                      "    \"manufacturer\": \"Airbus\",\n" +
                      "    \"specs\": {\n" +
                      "        \"fuelCapacityLiters\": 24210.0,\n" +
                      "        \"maximumRangeKm\": 6150.0,\n" +
                      "        \"cruisingSpeedKph\": 828.0,\n" +
                      "        \"standardSeatingCapacity\": 180\n" +
                      "    }\n" +
                      "}\n";

        mockMvc.perform(post("/api/catalog/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500WhenServiceFails() throws Exception {

        doThrow(new RuntimeException("Database failure"))
                .when(addModelUseCase)
                .execute(any());

        String json = "{\n" +
                      "    \"modelName\": \"Airbus A320\",\n" +
                      "    \"modelImage\": [\n" +
                      "        \"https://example.com/a320.jpg\"\n" +
                      "    ],\n" +
                      "    \"manufacturer\": \"Airbus\",\n" +
                      "    \"specs\": {\n" +
                      "        \"fuelCapacityLiters\": 24210.0,\n" +
                      "        \"maximumRangeKm\": 6150.0,\n" +
                      "        \"cruisingSpeedKph\": 828.0,\n" +
                      "        \"standardSeatingCapacity\": 180\n" +
                      "    }\n" +
                      "}\n";

        mockMvc.perform(post("/api/catalog/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }
}