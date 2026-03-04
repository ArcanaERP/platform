package com.arcanaerp.platform.identity.web;

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
class OrgUnitsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsOrgUnitsByTenantWithPagination() throws Exception {
        String tenantCode = "tenou01";

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 01",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENOU01"))
            .andExpect(jsonPath("$.code").value("OPS"))
            .andExpect(jsonPath("$.name").value("Operations"));

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 01",
            "hr",
            "Human Resources"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENOU01"))
            .andExpect(jsonPath("$.code").value("HR"))
            .andExpect(jsonPath("$.name").value("Human Resources"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.code=='OPS')].name", hasItem("Operations")))
            .andExpect(jsonPath("$.items[?(@.code=='HR')].name", hasItem("Human Resources")))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='TENOU01')].tenantName", hasItem("Tenant OU 01")));
    }
}
