package com.sidis.aircraftservice.ControllersTest;

import com.sidis.aircraftservice.Aircraft.HangarViewController;
import com.sidis.aircraftservice.Aircraft.application.CalculationsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HangarViewController.class)
@AutoConfigureMockMvc
@Import(HangarViewControllerTest.MethodSecurityTestConfig.class)
class HangarViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculationsService calculationsService;

    private static final String URL = "/api/hangar/view/utilization";

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn200AndUtilizationView() throws Exception {
        when(calculationsService.getUtilizationInfo())
                .thenReturn(List.of());
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("utilization"))
                .andExpect(model().attributeExists("utilizationData"));
        verify(calculationsService, times(1)).getUtilizationInfo();
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldReturn200WithEmptyUtilizationData() throws Exception {
        when(calculationsService.getUtilizationInfo()).thenReturn(List.of());
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("utilizationData", List.of()));
        verify(calculationsService).getUtilizationInfo();
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserDoesNotHaveBackofficeRole() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(calculationsService);
    }

    @Test
    void shouldRejectRequestWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(calculationsService);
    }

    @Test
    @WithMockUser(roles = "BACKOFFICE")
    void shouldHandleServiceFailure() throws Exception {
        when(calculationsService.getUtilizationInfo()).thenThrow(new RuntimeException("Calculation failed"));
        var result = mockMvc.perform(get(URL)).andReturn();
        assertNotNull(result.getResolvedException());
        assertInstanceOf(RuntimeException.class, result.getResolvedException());
        verify(calculationsService).getUtilizationInfo();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}