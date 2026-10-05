package com.sidis.maintenanceservice.WP4A.US119;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
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
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
//import com.sidis.maintenanceservice.authUsers.application.JwtService;
//import com.sidis.maintenanceservice.authUsers.infrastructure.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceRecordHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockBean private MaintenanceReportService maintenanceReportService;
    @MockBean private MaintenanceAlertService maintenanceAlertService;
    @MockBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
//    @MockBean private AircraftSearchService aircraftSearchService;
    @MockBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
    @MockBean private PartsInventoryService partsInventoryService;
//    @MockBean private JwtService jwtService;
//    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    /*@Test
    void shouldGetRecordByIdSuccessfully() throws Exception {
//        Aircraft aircraft = mock(Aircraft.class);
//        when(aircraft.getRegistrationNumber()).thenReturn("CS-TVA");

        MaintenanceTemplate template = new MaintenanceTemplate("Engine Check", 2.0, java.util.Map.of(), com.sidis.maintenanceservice.domain.MaintenanceType.INSPECTION, com.sidis.maintenanceservice.domain.MaintenanceAttribute.ENGINE);

        MaintenanceRecord record = new MaintenanceRecord("CS-TVA", template, 2.0, "desc", LocalDate.of(2026,5,1));

        when(maintenanceRecordProgressService.findById(1L)).thenReturn(Optional.of(record));

        mockMvc.perform(get("/api/maintenance/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraftRegistration").value("CS-TVA"))
                .andExpect(jsonPath("$.description").value("desc"));

        verify(maintenanceRecordProgressService, times(1)).findById(1L);
    }*/

    @Test
    void shouldAdvanceStatusSuccessfully() throws Exception {
        /*Aircraft aircraft = mock(Aircraft.class);
        when(aircraft.getRegistrationNumber()).thenReturn("CS-TVA");*/

        MaintenanceTemplate template = new MaintenanceTemplate("Engine Check", 2.0, java.util.Map.of(), com.sidis.maintenanceservice.domain.MaintenanceType.INSPECTION, com.sidis.maintenanceservice.domain.MaintenanceAttribute.ENGINE);

        MaintenanceRecord updated = new MaintenanceRecord("CS-TVA", template, 2.0, "desc", LocalDate.of(2026,5,1));
        // simulate status change
        updated.markAsOngoing();

        when(maintenanceRecordProgressService.advanceStatus(1L, MaintenanceStatus.ONGOING)).thenReturn(updated);

        mockMvc.perform(patch("/api/maintenance/records/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"));

        verify(maintenanceRecordProgressService, times(1)).advanceStatus(1L, MaintenanceStatus.ONGOING);
    }

    @Test
    void shouldReturn409WhenRecordNotFound() throws Exception {
        when(maintenanceRecordProgressService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/maintenance/records/99"))
                .andExpect(status().isConflict());

        verify(maintenanceRecordProgressService, times(1)).findById(99L);
    }
}


