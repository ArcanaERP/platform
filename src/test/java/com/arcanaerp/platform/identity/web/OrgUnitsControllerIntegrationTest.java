package com.arcanaerp.platform.identity.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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

    @Test
    void filtersOrgUnitsByActiveFlag() throws Exception {
        String tenantCode = "tenou07";

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 07",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 07",
            "hr",
            "Human Resources"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.updateOrgUnit(
            mockMvc,
            tenantCode,
            "hr",
            "Human Resources",
            false
        )
            .andExpect(status().isOk());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 0, 10, "active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("OPS"))
            .andExpect(jsonPath("$.items[0].active").value(true));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 0, 10, "active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("HR"))
            .andExpect(jsonPath("$.items[0].active").value(false));
    }

    @Test
    void rejectsInvalidActiveQueryParameter() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest("tenou08", 0, 10, "active", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("active query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest("tenou08", 0, 10, "active", "yes"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("active query parameter must be either true or false"))
            .andExpect(jsonPath("$.path").value("/api/identity/org-units"));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            "tenou09",
            "Tenant OU 09",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest("tenou09"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='TENOU09')].code", hasItem("OPS")));
    }

    @Test
    void paginatesOrgUnitsAtPageBoundaries() throws Exception {
        String tenantCode = "tenou10";

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 10",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 10",
            "hr",
            "Human Resources"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 0, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 1, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void combinesActiveFilterWithPagination() throws Exception {
        String tenantCode = "tenou11";

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 11",
            "ops",
            "Operations"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 11",
            "hr",
            "Human Resources"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createOrgUnit(
            mockMvc,
            tenantCode,
            "Tenant OU 11",
            "fin",
            "Finance"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.updateOrgUnit(mockMvc, tenantCode, "hr", "Human Resources", false)
            .andExpect(status().isOk());
        IdentityWebIntegrationTestSupport.updateOrgUnit(mockMvc, tenantCode, "fin", "Finance", false)
            .andExpect(status().isOk());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 0, 1, "active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].active").value(false));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listOrgUnitsRequest(tenantCode, 1, 1, "active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items[0].active").value(false));
    }
}
