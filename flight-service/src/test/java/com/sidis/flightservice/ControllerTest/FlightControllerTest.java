package com.sidis.flightservice.ControllerTest;

import com.sidis.flightservice.Flight.application.AircraftSearchService;
import com.sidis.flightservice.Flight.application.FlightQueryService;
import com.sidis.flightservice.Flight.application.FlightService;
import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.domain.FlightStatus;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftClientDTO;

import com.sidis.flightservice.FlightController;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightController.class)
@AutoConfigureMockMvc
class FlightControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlightService flightService;

    @MockBean
    private AircraftSearchService aircraftSearchService;

    @MockBean
    private FlightQueryService flightQueryService;

    // GET /api/flights
    @Test
    @WithMockUser
    void shouldGetAllFlightsSuccessfully() throws Exception {
        when(flightQueryService.findAll())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(flightQueryService).findAll();
    }

    @Test
    @WithMockUser
    void shouldReturnFlightsSuccessfully() throws Exception {
        FlightReadModel flight = mock(FlightReadModel.class);
        when(flight.flightId()).thenReturn(1L);
        when(flight.route()).thenReturn(null);
        when(flight.aircraftRegistrationNumber()).thenReturn("CS-TUA");
        when(flight.status()).thenReturn(FlightStatus.SCHEDULED);
        when(flight.scheduledDeparture()).thenReturn(LocalDateTime.of(2026, 12, 10, 10, 0));
        when(flight.scheduledArrival()).thenReturn(LocalDateTime.of(2026, 12, 10, 12, 0));
        when(flightQueryService.findAll()).thenReturn(List.of(flight));
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flightId").value(1))
                .andExpect(jsonPath("$[0].aircraftRegistrationNumber")
                        .value("CS-TUA"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].routeId").isEmpty());
    }

    @Test
    @WithMockUser
    void shouldReturn500WhenGetAllFlightsFails() throws Exception {
        when(flightQueryService.findAll()).thenThrow(new RuntimeException("Unexpected error"));
        mockMvc.perform(get("/api/flights")).andExpect(status().isInternalServerError());
    }

    // POST /api/flights/scheduled
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn400WhenScheduleFlightRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/flights/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aircraftSearchService, flightService);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn400WhenAircraftRegistrationNumberIsBlank() throws Exception {
        String json = """
                {
                    "routeId": 1,
                    "aircraftRegistrationNumber": "",
                    "scheduledDeparture": "2026-12-10T10:00:00",
                    "scheduledArrival": "2026-12-10T12:00:00"
                }
                """;
        mockMvc.perform(post("/api/flights/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aircraftSearchService, flightService);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn409WhenAircraftDoesNotExist() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("CS-TUA"))
                .thenReturn(Optional.empty());
        mockMvc.perform(post("/api/flights/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScheduleFlightJson()))
                .andExpect(status().isConflict());
        verify(aircraftSearchService).spotAircraftInHangar("CS-TUA");
        verifyNoInteractions(flightService);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn409WhenAircraftModelDoesNotExist() throws Exception {
        AircraftClientDTO aircraft = mock(AircraftClientDTO.class);
        when(aircraft.modelName()).thenReturn("A320");
        when(aircraftSearchService.spotAircraftInHangar("CS-TUA")).thenReturn(Optional.of(aircraft));
        when(aircraftSearchService.spotAircraftModelInCatalog("A320")).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/flights/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScheduleFlightJson()))
                .andExpect(status().isConflict());
        verifyNoInteractions(flightService);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn500WhenAircraftSearchFails() throws Exception {
        when(aircraftSearchService.spotAircraftInHangar("CS-TUA"))
                .thenThrow(new RuntimeException("Unexpected error"));
        mockMvc.perform(post("/api/flights/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScheduleFlightJson()))
                .andExpect(status().isInternalServerError());
        verifyNoInteractions(flightService);
    }

    // GET /api/flights/aircraft/{registrationNumber}
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetScheduledFlightsForAircraftSuccessfully() throws Exception {
        when(flightQueryService.getScheduledFlightsForAircraft("CS-TUA")).thenReturn(List.of());
        mockMvc.perform(get("/api/flights/aircraft/CS-TUA"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(flightQueryService).getScheduledFlightsForAircraft("CS-TUA");
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn500WhenGetScheduledFlightsFails() throws Exception {
        when(flightQueryService.getScheduledFlightsForAircraft("CS-TUA"))
                .thenThrow(new RuntimeException("Unexpected error"));
        mockMvc.perform(get("/api/flights/aircraft/CS-TUA"))
                .andExpect(status().isInternalServerError());
    }

    // GET /api/flights/reports/route-utilization
    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldGetRouteUtilizationReportSuccessfully() throws Exception {
        when(flightQueryService.getRouteUtilizationReport()).thenReturn(List.of());
        mockMvc.perform(get("/api/flights/reports/route-utilization"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(flightQueryService).getRouteUtilizationReport();
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn500WhenRouteUtilizationReportFails() throws Exception {
        when(flightQueryService.getRouteUtilizationReport()).thenThrow(new RuntimeException("Unexpected error"));
        mockMvc.perform(get("/api/flights/reports/route-utilization"))
                .andExpect(status().isInternalServerError());
    }

    private String validScheduleFlightJson() {
        return """
                {
                    "routeId": 1,
                    "aircraftRegistrationNumber": "CS-TUA",
                    "scheduledDeparture": "2026-12-10T10:00:00",
                    "scheduledArrival": "2026-12-10T12:00:00"
                }
                """;
    }
}
