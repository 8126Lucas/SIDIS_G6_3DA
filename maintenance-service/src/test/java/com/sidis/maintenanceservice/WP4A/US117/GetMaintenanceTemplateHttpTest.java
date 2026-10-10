package com.sidis.maintenanceservice.WP4A.US117;


import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.AddMaintenanceRecordUseCase;
import com.sidis.maintenanceservice.application.AddMaintenanceTemplateUseCase;
import com.sidis.maintenanceservice.application.MaintenanceAlertService;
import com.sidis.maintenanceservice.application.MaintenanceRecordProgressService;
import com.sidis.maintenanceservice.application.MaintenanceReportService;
import com.sidis.maintenanceservice.application.MaintenanceRecordSearchService;
import com.sidis.maintenanceservice.application.MaintenanceTemplateSearchService;
import com.sidis.maintenanceservice.application.PartsInventoryService;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class GetMaintenanceTemplateHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockitoBean private MaintenanceReportService maintenanceReportService;
    @MockitoBean private MaintenanceAlertService maintenanceAlertService;
    @MockitoBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockitoBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockitoBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
    @MockitoBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
    @MockitoBean private PartsInventoryService partsInventoryService;

    @Test
    void shouldGetTemplateByIdSuccessfully() throws Exception {
        MaintenanceTemplate template = new MaintenanceTemplate(
                "Engine Check",
                2.0,
                Map.of("oil_change", true),
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.ENGINE
        );

        when(maintenanceTemplateSearchService.findByTemplateId(1L)).thenReturn(Optional.of(template));

        mockMvc.perform(get("/api/maintenance/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Engine Check"))
                .andExpect(jsonPath("$.expectedDuration").value(2.0));

        verify(maintenanceTemplateSearchService, times(1)).findByTemplateId(1L);
    }

    @Test
    void shouldListTemplates() throws Exception {
        MaintenanceTemplate t1 = new MaintenanceTemplate(
                "Engine Check",
                2.0,
                Map.of("oil_change", true),
                MaintenanceType.INSPECTION,       // era NORMAL  → não existe
                MaintenanceAttribute.ENGINE        // era SAFETY  → não existe
        );

        MaintenanceTemplate t2 = new MaintenanceTemplate(
                "Landing Gear",
                3.5,
                Map.of("brakes", true),
                MaintenanceType.SCHEDULED,
                MaintenanceAttribute.AIRFRAME
        );

        when(maintenanceTemplateSearchService.findAll()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/maintenance/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Engine Check"))
                .andExpect(jsonPath("$[1].name").value("Landing Gear"));

        verify(maintenanceTemplateSearchService, times(1)).findAll();
    }

    @Test
    void shouldReturn409WhenTemplateNotFound() throws Exception {
        when(maintenanceTemplateSearchService.findByTemplateId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/maintenance/templates/99"))
                .andExpect(status().isConflict());

        verify(maintenanceTemplateSearchService, times(1)).findByTemplateId(99L);
    }
}