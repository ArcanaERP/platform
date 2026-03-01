package com.arcanaerp.platform.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndInfoEndpointsAreExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap());
    }

    @Test
    void nonExposedActuatorEndpointsAreUnavailable() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isNotFound());
    }

    @Test
    void actuatorRootEndpointIsUnavailable() throws Exception {
        mockMvc.perform(get("/actuator"))
            .andExpect(status().isNotFound());
    }
}
