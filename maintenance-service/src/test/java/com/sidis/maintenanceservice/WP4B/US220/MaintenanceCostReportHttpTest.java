package com.sidis.maintenanceservice.WP4B.US220;

//import com.sidis.maintenanceservice.Aircraft.application.AircraftSearchService;
import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.*;
//import com.sidis.maintenanceservice.authUsers.application.JwtService;
//import com.sidis.maintenanceservice.authUsers.infrastructure.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceCostReportHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private MaintenanceReportService maintenanceReportService;
    @MockitoBean private MaintenanceAlertService maintenanceAlertService;
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
    void shouldReturnMaintenanceCostReport() throws Exception {
        when(maintenanceReportService.maintenanceCosts("model")).thenReturn(List.of(
                new MaintenanceReportService.MaintenanceCostReport("Boeing 737", 250.0)
        ));

        mockMvc.perform(get("/api/maintenance/reports/costs")
                        .param("groupBy", "model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].group").value("Boeing 737"))
                .andExpect(jsonPath("$[0].totalCost").value(250.0));

        verify(maintenanceReportService, times(1)).maintenanceCosts("model");
    }*/
}
