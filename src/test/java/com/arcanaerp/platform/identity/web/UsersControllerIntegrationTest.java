package com.arcanaerp.platform.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UsersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsUser() throws Exception {
        String payload = """
            {
              "tenantCode": "acme01",
              "tenantName": "Acme 01",
              "roleCode": "admin",
              "roleName": "Administrator",
              "email": "OPS01@ACME.COM",
              "displayName": "Ops User"
            }
            """;

        mockMvc.perform(post("/api/identity/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("ACME01"))
            .andExpect(jsonPath("$.roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("ops01@acme.com"));

        mockMvc.perform(get("/api/identity/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tenantCode").value("ACME01"))
            .andExpect(jsonPath("$[0].displayName").value("Ops User"));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateTenantEmail() throws Exception {
        String payload = """
            {
              "tenantCode": "acme02",
              "tenantName": "Acme 02",
              "roleCode": "admin",
              "roleName": "Administrator",
              "email": "ops02@acme.com",
              "displayName": "Ops User"
            }
            """;

        mockMvc.perform(post("/api/identity/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/identity/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/identity/users"));
    }
}
