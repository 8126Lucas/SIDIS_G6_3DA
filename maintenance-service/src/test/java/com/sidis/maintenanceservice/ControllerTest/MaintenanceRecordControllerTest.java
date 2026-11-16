package com.sidis.maintenanceservice.ControllerTest;

import com.sidis.maintenanceservice.MaintenanceRecordController;
import com.sidis.maintenanceservice.application.*;
import com.sidis.maintenanceservice.exceptions.GlobalExceptionHandler;
import com.sidis.maintenanceservice.infrastructure.client.AircraftClientDTO;
import com.sidis.maintenanceservice.infrastructure.client.AircraftRestClientService;
import com.sidis.maintenanceservice.infrastructure.client.PeerForwarder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceRecordController.class)
@Import({MaintenanceRecordControllerTest.MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
class MaintenanceRecordControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;

    @MockBean
    private AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;

    @MockBean
    private AircraftRestClientService aircraftClient;

    @MockBean
    private MaintenanceTemplateSearchService maintenanceTemplateSearchService;

    @MockBean
    private MaintenanceRecordProgressService maintenanceRecordProgressService;

    @MockBean
    private MaintenanceReportService maintenanceReportService;

    @MockBean
    private MaintenanceAlertService maintenanceAlertService;

    @MockBean
    private MaintenanceRecordSearchService maintenanceRecordSearchService;

    @MockBean
    private PartsInventoryService partsInventoryService;

    @MockBean
    private PeerForwarder peerForwarder;

    // LISTAR TEMPLATES
    @Test
    @WithMockUser
    void shouldListTemplatesSuccessfully() throws Exception {
        when(maintenanceTemplateSearchService.findAll())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/templates"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceTemplateSearchService).findAll();
    }

    // ATIVIDADES DE MANUTENÇÃO EM CURSO
    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldGetOngoingMaintenanceActivities() throws Exception {
        when(maintenanceReportService.ongoingMaintenanceActivities())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/ongoing"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceReportService).ongoingMaintenanceActivities();
    }

    // MÉDIA DE TEMPO DE MANUTENÇÃO
    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldGetTurnaroundAverage() throws Exception {
        when(maintenanceReportService.averageTurnaroundTimePerAircraftType())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/turnaround-average"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceReportService).averageTurnaroundTimePerAircraftType();
    }

    // CUSTOS DE MANUTENÇÃO
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetMaintenanceCosts() throws Exception {
        when(maintenanceReportService.maintenanceCosts("aircraft"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/reports/costs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceReportService).maintenanceCosts("aircraft");
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetMaintenanceCostsGroupedByModel() throws Exception {
        when(maintenanceReportService.maintenanceCosts("model"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/reports/costs")
                        .param("groupBy", "model"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceReportService).maintenanceCosts("model");
    }

    // ALERTAS DE MANUTENÇÃO
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetMaintenanceAlerts() throws Exception {
        when(maintenanceAlertService.findAircraftDueForMaintenance(180, 500.0))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceAlertService)
                .findAircraftDueForMaintenance(180, 500.0);
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetMaintenanceAlertsWithCustomThresholds() throws Exception {
        when(maintenanceAlertService.findAircraftDueForMaintenance(90, 300.0))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/alerts")
                        .param("calendarDaysThreshold", "90")
                        .param("flightHoursThreshold", "300"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceAlertService)
                .findAircraftDueForMaintenance(90, 300.0);
    }

    // RELATÓRIO DE HORAS
    @Test
    @WithMockUser(roles = "ATCC")
    void shouldGetMaintenanceHours() throws Exception {
        when(maintenanceReportService.totalMaintenanceHoursByAircraft())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/reports/hours"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(maintenanceReportService).totalMaintenanceHoursByAircraft();
    }

    // INVENTÁRIO DE PEÇAS
    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldGetPartsInventory() throws Exception {
        when(partsInventoryService.getPartsUsageSummary())
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/parts/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(partsInventoryService).getPartsUsageSummary();
    }

    // ALERTAS DE STOCK BAIXO
    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldGetLowStockAlerts() throws Exception {
        when(partsInventoryService.getLowStockAlerts(10))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/parts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(partsInventoryService).getLowStockAlerts(10);
    }

    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldGetLowStockAlertsWithCustomThreshold() throws Exception {
        when(partsInventoryService.getLowStockAlerts(20))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/maintenance/parts/low-stock")
                        .param("threshold", "20"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(partsInventoryService).getLowStockAlerts(20);
    }

    // CONSULTAR REGISTOS COM FILTROS

    @Test
    @WithMockUser
    void shouldGetMaintenanceRecords() throws Exception {
        when(aircraftClient.findAircraftByRegistration("CS-TST"))
                .thenReturn(Optional.of(mock(AircraftClientDTO.class)));
        when(maintenanceRecordSearchService.findRecordsMatchingFilter(eq("CS-TST"), isNull(),
                isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/api/maintenance/records")
                        .param("aircraft", "CS-TST")).andExpect(status().isOk());
        verify(aircraftClient).findAircraftByRegistration("CS-TST");
        verify(maintenanceRecordSearchService).findRecordsMatchingFilter(eq("CS-TST"), isNull(),
                isNull(), isNull(), any(Pageable.class));
    }

    // SEGURANÇA
    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotATCC() throws Exception {
        mockMvc.perform(get("/api/maintenance/reports/hours"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn403WhenUserIsNotMaintenanceSupervisor() throws Exception {
        mockMvc.perform(get("/api/maintenance/ongoing"))
                .andExpect(status().isForbidden());
    }


    // =====================================================
    // EDGE CASES - HTTP 400 BAD REQUEST
    // =====================================================

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn400WhenCalendarDaysThresholdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance/alerts")
                        .param("calendarDaysThreshold", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ATCC")
    void shouldReturn400WhenFlightHoursThresholdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance/alerts")
                        .param("flightHoursThreshold", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MAINTENANCE_SUPERVISOR")
    void shouldReturn400WhenLowStockThresholdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance/parts/low-stock")
                        .param("threshold", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenInitialDateIsInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance/records").param("initialDate", "invalid-date")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenPageIsInvalid() throws Exception {
        mockMvc.perform(get("/api/maintenance/records").param("page", "invalid")).andExpect(status().isBadRequest());
    }

    // EDGE CASES - HTTP 401 UNAUTHORIZED
    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/maintenance/templates"))
                .andExpect(status().isUnauthorized());
    }

    // EDGE CASES - HTTP 403 FORBIDDEN
    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotMaintenanceSupervisorForTurnaround() throws Exception {
        mockMvc.perform(get("/api/maintenance/turnaround-average")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotMaintenanceSupervisorForInventory() throws Exception {
        mockMvc.perform(get("/api/maintenance/parts/inventory")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotMaintenanceSupervisorForLowStock() throws Exception {
        mockMvc.perform(get("/api/maintenance/parts/low-stock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotATCCForCosts() throws Exception {
        mockMvc.perform(get("/api/maintenance/reports/costs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void shouldReturn403WhenUserIsNotATCCForAlerts() throws Exception {
        mockMvc.perform(get("/api/maintenance/alerts")).andExpect(status().isForbidden());
    }

    // EDGE CASES - HTTP 404 NOT FOUND
    @Test
    @WithMockUser
    void shouldReturn404WhenAircraftDoesNotExist() throws Exception {
        when(aircraftClient.findAircraftByRegistration("XX-XXX")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/maintenance/records").param("aircraft", "XX-XXX")).andExpect(status().isNotFound());
        verify(aircraftClient).findAircraftByRegistration("XX-XXX");
        verifyNoInteractions(maintenanceRecordSearchService);
    }

    //HTTP 500 INTERNAL SERVER ERROR
    @Test
    @WithMockUser
    void shouldReturn500WhenTemplateSearchServiceFails() throws Exception {
        when(maintenanceTemplateSearchService.findAll())
                .thenThrow(new RuntimeException("Unexpected database error"));

        mockMvc.perform(get("/api/maintenance/templates"))
                .andExpect(status().isInternalServerError());

        verify(maintenanceTemplateSearchService).findAll();
    }


    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
