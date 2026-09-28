package com.sidis.maintenanceservice.US226;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartsInventoryHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PartsInventoryService partsInventoryService;
    @MockBean private MaintenanceReportService maintenanceReportService;
    @MockBean private MaintenanceAlertService maintenanceAlertService;
    @MockBean private MaintenanceRecordProgressService maintenanceRecordProgressService;
    @MockBean private MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    @MockBean private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    @MockBean private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
//    @MockBean private AircraftSearchService aircraftSearchService;
    @MockBean private MaintenanceRecordSearchService maintenanceRecordSearchService;
//    @MockBean private JwtService jwtService;
//    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldReturnPartsInventorySummary() throws Exception {
        PartsInventoryService.PartUsageSummary summary =
                new PartsInventoryService.PartUsageSummary("BOLT-XL", 10, 50.0, 2L);

        when(partsInventoryService.getPartsUsageSummary()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/maintenance/parts/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partSerialNumber").value("BOLT-XL"))
                .andExpect(jsonPath("$[0].totalQuantityUsed").value(10))
                .andExpect(jsonPath("$[0].totalCost").value(50.0))
                .andExpect(jsonPath("$[0].timesUsedInRecords").value(2));

        verify(partsInventoryService, times(1)).getPartsUsageSummary();
    }

    @Test
    void shouldReturnEmptyInventoryWhenNoPartsUsed() throws Exception {
        when(partsInventoryService.getPartsUsageSummary()).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/parts/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnLowStockAlertsWithDefaultThreshold() throws Exception {
        PartsInventoryService.LowStockAlert alert = new PartsInventoryService.LowStockAlert(
                "FILTER-A", 15, 3L,
                "Part FILTER-A used 15 units across 3 record(s); consider restocking (threshold: 10)"
        );

        when(partsInventoryService.getLowStockAlerts(10)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/maintenance/parts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partSerialNumber").value("FILTER-A"))
                .andExpect(jsonPath("$[0].totalQuantityUsed").value(15))
                .andExpect(jsonPath("$[0].timesUsedInRecords").value(3))
                .andExpect(jsonPath("$[0].alertMessage").value(
                        "Part FILTER-A used 15 units across 3 record(s); consider restocking (threshold: 10)"));

        verify(partsInventoryService, times(1)).getLowStockAlerts(10);
    }

    @Test
    void shouldAcceptCustomThreshold() throws Exception {
        when(partsInventoryService.getLowStockAlerts(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/parts/low-stock").param("threshold", "5"))
                .andExpect(status().isOk());

        verify(partsInventoryService, times(1)).getLowStockAlerts(5);
    }

    @Test
    void shouldReturnEmptyAlertsWhenNoLowStock() throws Exception {
        when(partsInventoryService.getLowStockAlerts(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/maintenance/parts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
