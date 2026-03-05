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

    @Test
    void readsOrgUnitByTenantAndCode() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou02",
            "Tenant OU 02",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.getOrgUnitRequest("tenou02", "ops"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENOU02"))
            .andExpect(jsonPath("$.code").value("OPS"))
            .andExpect(jsonPath("$.name").value("Operations"));
    }

    @Test
    void returnsNotFoundForMissingOrgUnitByTenantAndCode() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou03",
            "Tenant OU 03",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.getOrgUnitRequest("tenou03", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Org unit not found for tenant/code: TENOU03/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units/missing"));
    }

    @Test
    void updatesOrgUnitNameAndActiveFlag() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou04",
            "Tenant OU 04",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.updateOrgUnit(
            mockMvc,
            "tenou04",
            "ops",
            "Operations West",
            false
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENOU04"))
            .andExpect(jsonPath("$.code").value("OPS"))
            .andExpect(jsonPath("$.name").value("Operations West"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(IdentityWebIntegrationTestSupport.getOrgUnitRequest("tenou04", "ops"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Operations West"))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void rejectsInvalidUpdateOrgUnitPayload() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou05",
            "Tenant OU 05",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.updateOrgUnit(
            mockMvc,
            "tenou05",
            "ops",
            "   ",
            false
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units/ops"));

        IdentityWebIntegrationTestSupport.updateOrgUnit(
            mockMvc,
            "tenou05",
            "ops",
            "Operations East",
            null
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units/ops"));
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingOrgUnit() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou06",
            "Tenant OU 06",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.updateOrgUnit(
            mockMvc,
            "tenou06",
            "missing",
            "Operations Missing",
            true
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Org unit not found for tenant/code: TENOU06/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units/missing"));
    }
}
