package com.arcanaerp.platform.identity.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TenantsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTenantsCreatedThroughIdentityFlows() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "tenantweb01",
            "Tenant Web 01",
            "admin",
            "Administrator"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "tenantweb02",
            "Tenant Web 02",
            "operator",
            "Operator",
            "tenantweb02@acme.com",
            "Tenant Web User"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listTenantsRequest(0, 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.items[?(@.code=='TENANTWEB01')].name", hasItem("Tenant Web 01")))
            .andExpect(jsonPath("$.items[?(@.code=='TENANTWEB02')].name", hasItem("Tenant Web 02")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "tenantweb03",
            "Tenant Web 03",
            "analyst",
            "Analyst"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listTenantsRequest())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.listTenantsRequest(-1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listTenantsRequest(0, 0))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }
}
