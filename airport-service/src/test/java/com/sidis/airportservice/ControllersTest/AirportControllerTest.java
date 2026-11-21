
package com.sidis.airportservice.ControllersTest;

import com.sidis.airportservice.Airport.AirportController;
import com.sidis.airportservice.Airport.application.*;
import com.sidis.airportservice.Airport.domain.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.boot.test.context.TestConfiguration;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AirportController.class)
@AutoConfigureMockMvc
class AirportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AirportService airportService;

    @MockBean
    private AirportCsvService airportCsvService;

    @MockBean
    private AirportQueryService airportQueryService;

    private Airport airport;

    @BeforeEach
    void setUp() {
        airport = mock(Airport.class);
        when(airport.getIataCode()).thenReturn("OPO");
        when(airport.getAirportType()).thenReturn("International");
        when(airport.getName()).thenReturn("Francisco Sa Carneiro");
        when(airport.getStatus()).thenReturn(AirportStatus.values()[0]);
        when(airport.getRouteDistance()).thenReturn(500.0);
        when(airport.getAirportPhotos()).thenReturn(List.of());
        when(airport.getOperationalHours()).thenReturn("24/7");

        AirportLocation location = mock(AirportLocation.class);
        when(location.getCity()).thenReturn("Porto");
        when(location.getLongitude()).thenReturn(-8.67);
        when(location.getLatitude()).thenReturn(41.24);
        when(location.getRegion()).thenReturn("Norte");
        when(location.getTimezone()).thenReturn("Europe/Lisbon");
        when(location.getCountry()).thenReturn("Portugal");

        Facilities facilities = mock(Facilities.class);
        when(facilities.getTerminalCount()).thenReturn(2);
        when(facilities.getServices()).thenReturn(List.of("WiFi"));
        when(facilities.getGateCount()).thenReturn(20);

        when(airport.getAirportLocation()).thenReturn(location);
        when(airport.getFacilities()).thenReturn(facilities);
        when(airport.getAirportRunways()).thenReturn(List.of());
        when(airport.getCertifications()).thenReturn(List.of());
        when(airport.getContacts()).thenReturn(List.of());
    }

    // POST /api/airports
    @Test
    @WithMockUser
    void shouldRegisterAirportSuccessfully() throws Exception {
        String json = """
        {
            "iataCode": "OPO",
            "airportType": "International",
            "name": "Francisco Sa Carneiro",
            "status": "%s",
            "location": {
                "city": "Porto",
                "longitude": -8.67,
                "latitude": 41.24,
                "region": "Norte",
                "timezone": "Europe/Lisbon",
                "country": "Portugal"
            },
            "facilities": {
                "terminalCount": 2,
                "services": ["WiFi"],
                "gateCount": 20
            },
            "routeDistance": 500.0,
            "runways": [],
            "certifications": [],
            "contacts": [],
            "airportPhotos": []
        }
        """.formatted(AirportStatus.values()[0].name());
        mockMvc.perform(post("/api/airports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        verify(airportService).execute(any(Airport.class));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenAirportJsonIsInvalid() throws Exception {
        mockMvc.perform(post("/api/airports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(airportService);
    }

    // POST /api/airports/{iataCode}/certifications
    @Test
    @WithMockUser
    void shouldAddCertificationSuccessfully() throws Exception {
        Certification certification = mock(Certification.class);
        when(certification.getName()).thenReturn("ISO 9001");
        when(certification.getCategory()).thenReturn("QUALITY");
        when(certification.getStartsAt()).thenReturn(LocalDate.of(2025, 1, 1));
        when(certification.getExpiresAt()).thenReturn(LocalDate.of(2027, 1, 1));

        when(airportService.addCertification(eq("OPO"), anyString(), anyString(), any(), any())).thenReturn(certification);
        String json = """
        {
            "name": "ISO 9001",
            "category": "QUALITY",
            "startsAt": "2025-01-01",
            "expiresAt": "2027-01-01"
        }
        """;
        mockMvc.perform(post("/api/airports/OPO/certifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ISO 9001"))
                .andExpect(jsonPath("$.category").value("QUALITY"));
        verify(airportService).addCertification(
                eq("OPO"),
                eq("ISO 9001"),
                eq("QUALITY"),
                eq(LocalDate.of(2025, 1, 1)),
                eq(LocalDate.of(2027, 1, 1)));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenCertificationExpiryIsBeforeStart() throws Exception {
        String json = """
        {
            "name": "ISO 9001",
            "category": "QUALITY",
            "startsAt": "2027-01-01",
            "expiresAt": "2025-01-01"
        }
        """;
        mockMvc.perform(post("/api/airports/OPO/certifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
        verify(airportService, never()).addCertification(
                anyString(), anyString(), anyString(), any(), any());
    }

    // GET /api/airports/{iataCode}
    @Test
    @WithMockUser
    void shouldGetAirportSuccessfully() throws Exception {
        AirportReadModel readModel = mock(AirportReadModel.class);

        when(readModel.iataCode()).thenReturn("OPO");
        when(readModel.airportType()).thenReturn("International");
        when(readModel.name()).thenReturn("Francisco Sa Carneiro");
        when(readModel.status()).thenReturn(AirportStatus.values()[0]);
        when(readModel.location()).thenReturn(null);
        when(readModel.facilities()).thenReturn(null);
        when(readModel.routeDistance()).thenReturn(500.0);
        when(readModel.runways()).thenReturn(List.of());
        when(readModel.certifications()).thenReturn(List.of());
        when(readModel.contacts()).thenReturn(List.of());
        when(readModel.photos()).thenReturn(List.of());
        when(readModel.operationalHours()).thenReturn("24/7");

        when(airportQueryService.getAirportByIata("OPO")).thenReturn(Optional.of(readModel));
        mockMvc.perform(get("/api/airports/OPO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("OPO"))
                .andExpect(jsonPath("$.name").value("Francisco Sa Carneiro"));
    }

    @Test
    @WithMockUser
    void shouldHandleAirportNotFound() throws Exception {
        when(airportQueryService.getAirportByIata("XXX")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/airports/XXX")).andExpect(status().isNotFound());
    }

    // GET /api/airports
    @Test
    @WithMockUser
    void shouldSearchAirportsSuccessfully() throws Exception {
        when(airportQueryService.searchAirports(
                "Porto", "Portugal", null))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/airports")
                        .param("city", "Porto")
                        .param("country", "Portugal"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(airportQueryService).searchAirports("Porto", "Portugal", null);
    }

    @Test
    @WithMockUser
    void shouldSearchAirportsWithoutFilters() throws Exception {
        when(airportQueryService.searchAirports(null, null, null)).thenReturn(List.of());
        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // PATCH /api/airports/{iataCode}/status
    @Test
    @WithMockUser
    void shouldUpdateAirportStatusSuccessfully() throws Exception {
        when(airportService.updateStatus(
                eq("OPO"), any(AirportStatus.class)))
                .thenReturn(airport);
        String json = """
        {
            "status": "%s"
        }
        """.formatted(AirportStatus.values()[0].name());
        mockMvc.perform(patch("/api/airports/OPO/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("OPO"));
        verify(airportService).updateStatus(
                "OPO", AirportStatus.values()[0]);
    }

    // PATCH /api/airports/{iataCode}/details
    @Test
    @WithMockUser
    void shouldUpdateAirportDetailsSuccessfully() throws Exception {
        when(airportService.updateDetails(
                eq("OPO"), anyString(), anyString(), anyList()))
                .thenReturn(airport);
        String json = """
        {
            "name": "Porto International Airport",
            "operationalHours": "24/7",
            "contacts": []
        }
        """;
        mockMvc.perform(patch("/api/airports/OPO/details")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("OPO"));
        verify(airportService).updateDetails(
                eq("OPO"),
                eq("Porto International Airport"),
                eq("24/7"),
                anyList());
    }

    // GET /api/airports/{iataCode}/routes
    @Test
    @WithMockUser
    void shouldGetAirportRoutesSuccessfully() throws Exception {
        when(airportQueryService.findRoutesByAirport("OPO")).thenReturn(List.of());
        mockMvc.perform(get("/api/airports/OPO/routes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(airportQueryService).findRoutesByAirport("OPO");
    }

    // GET /api/airports/busiest
    @Test
    @WithMockUser
    void shouldGetBusiestAirportsSuccessfully() throws Exception {
        when(airportQueryService.busiestAirports()).thenReturn(List.of());
        mockMvc.perform(get("/api/airports/busiest"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // GET /api/airports/grouped
    @Test
    @WithMockUser
    void shouldGetAirportsGroupedByCountryByDefault() throws Exception {
        when(airportQueryService.groupAirportsBy("country")).thenReturn(Map.of());
        mockMvc.perform(get("/api/airports/grouped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
        verify(airportQueryService).groupAirportsBy("country");
    }

    @Test
    @WithMockUser
    void shouldGetAirportsGroupedByCity() throws Exception {
        when(airportQueryService.groupAirportsBy("city")).thenReturn(Map.of());
        mockMvc.perform(get("/api/airports/grouped")
                        .param("by", "city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    // POST /api/airports/import
    @Test
    @WithMockUser
    void shouldImportCsvSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "airports.csv",
                "text/csv",
                "iataCode,name\nOPO,Porto Airport".getBytes()
        );
        when(airportCsvService.importFile(any())).thenReturn(List.of(airport));
        mockMvc.perform(multipart("/api/airports/import")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(airportCsvService).importFile(any());
        verify(airportService).execute(airport);
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenCsvFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/airports/import")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(airportCsvService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
