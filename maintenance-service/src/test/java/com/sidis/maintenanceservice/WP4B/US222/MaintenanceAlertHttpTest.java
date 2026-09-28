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
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean private MaintenanceAlertService maintenanceAlertService;
    @MockBean private MaintenanceReportService maintenanceReportService;
    @MockBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
//    @MockBean private AircraftSearchService aircraftSearchService;
    @MockBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
    @MockBean private PartsInventoryService partsInventoryService;
//    @MockBean private JwtService jwtService;
//    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

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