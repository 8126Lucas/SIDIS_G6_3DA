package com.sidis.maintenanceservice.WP4B.US222;

//import com.sidis.maintenanceservice.Aircraft.application.AircraftSearchService;
import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.AddMaintenanceRecordUseCase;
import com.sidis.maintenanceservice.application.AddMaintenanceTemplateUseCase;
import com.sidis.maintenanceservice.application.MaintenanceAlertService;
import com.sidis.maintenanceservice.application.MaintenanceRecordProgressService;
import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.application.MaintenanceRecordSearchService;
import com.sidis.maintenanceservice.application.MaintenanceTemplateSearchService;
import com.sidis.maintenanceservice.application.PartsInventoryService;
//import com.sidis.maintenanceservice.authUsers.application.JwtService;
//import com.sidis.maintenanceservice.authUsers.infrastructure.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceAlertHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private MaintenanceAlertService maintenanceAlertService;
    @MockitoBean private MaintenanceReportService maintenanceReportService;
    @MockitoBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockitoBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockitoBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockitoBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
//    @MockitoBean private AircraftSearchService aircraftSearchService;
    @MockitoBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
    @MockitoBean private PartsInventoryService partsInventoryService;
//    @MockitoBean private JwtService jwtService;
//    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    /*@Test
    void shouldReturnAlertsWithDefaultThresholds() throws Exception {
        MaintenanceAlertService.MaintenanceAlert alert = new MaintenanceAlertService.MaintenanceAlert(
                "CS-TVA", "Boeing 737", 650.0,
                LocalDate.of(2025, 10, 1), 251L,
                "Total flight hours 650.0 reached threshold of 500.0"
        );

        when(maintenanceAlertService.findAircraftDueForMaintenance(180, 500.0)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/maintenance/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("CS-TVA"))
                .andExpect(jsonPath("$[0].aircraftModel").value("Boeing 737"))
                .andExpect(jsonPath("$[0].totalFlightHours").value(650.0))
                .andExpect(jsonPath("$[0].daysSinceLastMaintenance").value(251));

        verify(maintenanceAlertService, times(1)).findAircraftDueForMaintenance(180, 500.0);
    }*/

    /*@Test
    void shouldReturnEmptyWhenNoAlerts() throws Exception {
        when(maintenanceAlertService.findAircraftDueForMaintenance(180, 500.0)).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }*/

    /*@Test
    void shouldAcceptCustomThresholds() throws Exception {
        when(maintenanceAlertService.findAircraftDueForMaintenance(90, 300.0)).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/alerts")
                        .param("calendarDaysThreshold", "90")
                        .param("flightHoursThreshold", "300"))
                .andExpect(status().isOk());

        verify(maintenanceAlertService, times(1)).findAircraftDueForMaintenance(90, 300.0);
    }*/
}