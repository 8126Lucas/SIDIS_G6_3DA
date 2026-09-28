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
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean private MaintenanceReportService maintenanceReportService;
    @MockBean private MaintenanceAlertService maintenanceAlertService;
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
