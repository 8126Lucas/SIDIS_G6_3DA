package com.sidis.maintenanceservice.WP4B.US219;

//import com.sidis.maintenanceservice.Aircraft.application.AircraftSearchService;
//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.*;
import com.sidis.maintenanceservice.domain.*;
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
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class OngoingMaintenanceHttpTest {

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
    void shouldReturnOngoingMaintenanceActivities() throws Exception {
        MaintenanceRecord record = record();
        record.markAsOngoing();
        when(maintenanceReportService.ongoingMaintenanceActivities())
                .thenReturn(List.of(record));

        mockMvc.perform(get("/api/maintenance/ongoing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aircraftRegistration").value("CS-TVA"))
                .andExpect(jsonPath("$[0].status").value("ONGOING"));

        verify(maintenanceReportService, times(1)).ongoingMaintenanceActivities();
    }*/

    private MaintenanceRecord record() {
        /*Aircraft aircraft = new Aircraft("CS-TVA",
                new AircraftModel("Boeing 737", "Boeing", null, List.of()),
                LocalDate.of(2018, 1, 1), 0.0, 0.0);*/
        MaintenanceTemplate template = new MaintenanceTemplate("Engine Check", 2.0,
                Map.of(), MaintenanceType.INSPECTION, MaintenanceAttribute.ENGINE);
        return new MaintenanceRecord("CS-TVA", template, 6.0,
                "ongoing check", LocalDate.of(2026, 1, 1));
    }
}
