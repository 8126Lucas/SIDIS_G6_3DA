package com.sidis.maintenanceservice.WP4B.US221;


import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.AddMaintenanceRecordUseCase;
import com.sidis.maintenanceservice.application.AddMaintenanceTemplateUseCase;
import com.sidis.maintenanceservice.application.MaintenanceAlertService;
import com.sidis.maintenanceservice.application.MaintenanceRecordProgressService;
import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.application.MaintenanceRecordSearchService;
import com.sidis.maintenanceservice.application.MaintenanceTemplateSearchService;
import com.sidis.maintenanceservice.application.PartsInventoryService;
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
class TurnaroundAverageHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MaintenanceReportService maintenanceReportService;
    @MockBean private MaintenanceAlertService maintenanceAlertService;
    @MockBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
    @MockBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
    @MockBean private PartsInventoryService partsInventoryService;

    /*@Test
    void shouldReturnTurnaroundAverages() throws Exception {
        when(maintenanceReportService.averageTurnaroundTimePerAircraftType()).thenReturn(List.of(
                new MaintenanceReportService.TurnaroundAverage("Airbus A320", 3.0),
                new MaintenanceReportService.TurnaroundAverage("Boeing 737", 7.5)
        ));

        mockMvc.perform(get("/api/maintenance/turnaround-average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aircraftType").value("Airbus A320"))
                .andExpect(jsonPath("$[0].averageDays").value(3.0))
                .andExpect(jsonPath("$[1].aircraftType").value("Boeing 737"))
                .andExpect(jsonPath("$[1].averageDays").value(7.5));

        verify(maintenanceReportService, times(1)).averageTurnaroundTimePerAircraftType();
    }

    @Test
    void shouldReturnEmptyListWhenNoData() throws Exception {
        when(maintenanceReportService.averageTurnaroundTimePerAircraftType()).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/turnaround-average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }*/
}