package com.sidis.aircraftservice.WP1A.US104;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GetAircraftWithFilteringHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAircraftWhenAllFiltersMatch() throws Exception {

        mockMvc.perform(get("/api/hangar/aircraft")
                        .param("model", "737-800")
                        .param("status", "INACTIVE")
                        .param("manufacturingDate", "2018-05-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].registrationNumber").value("CS-TUA"))
                .andExpect(jsonPath("$[0].modelName").value("737-800"))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));
    }

    @Test
    void shouldReturnEmptyWhenModelDoesNotMatch() throws Exception {

        mockMvc.perform(get("/api/hangar/aircraft")
                        .param("model", "INVALID-MODEL")
                        .param("status", "INACTIVE")
                        .param("manufacturingDate", "2018-05-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnEmptyWhenStatusDoesNotMatch() throws Exception {

        mockMvc.perform(get("/api/hangar/aircraft")
                        .param("model", "Boeing 737-800")
                        .param("status", "MAINTENANCE")
                        .param("manufacturingDate", "2018-05-10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnEmptyWhenDateDoesNotMatch() throws Exception {

        mockMvc.perform(get("/api/hangar/aircraft")
                        .param("model", "Boeing 737-800")
                        .param("status", "INACTIVE")
                        .param("manufacturingDate", "2000-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}