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
class RolesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsRolesByTenant() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "roleweb00",
            "Role Web 00",
            "admin",
            "Administrator"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("ROLEWEB00"))
            .andExpect(jsonPath("$.code").value("ADMIN"))
            .andExpect(jsonPath("$.name").value("Administrator"));

        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "roleweb00",
            "Role Web 00",
            "analyst",
            "Analyst"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("roleweb00", 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ROLEWEB00')].code", hasItem("ADMIN")))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ROLEWEB00')].code", hasItem("ANALYST")));
    }

    @Test
    void listsRolesByTenant() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "roleweb01",
            "Role Web 01",
            "admin",
            "Administrator",
            "roleweb01-admin@acme.com",
            "Role Admin"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "roleweb01",
            "Role Web 01",
            "analyst",
            "Analyst",
            "roleweb01-analyst@acme.com",
            "Role Analyst"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("roleweb01", 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ROLEWEB01')].code", hasItem("ADMIN")))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ROLEWEB01')].code", hasItem("ANALYST")));
    }

    @Test
    void readsRoleByTenantAndCode() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "roleweb04",
            "Role Web 04",
            "admin",
            "Administrator"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.getRoleRequest("roleweb04", "admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("ROLEWEB04"))
            .andExpect(jsonPath("$.code").value("ADMIN"))
            .andExpect(jsonPath("$.name").value("Administrator"));
    }

    @Test
    void rejectsDuplicateRoleCodeInTenant() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "rolewebdup",
            "Role Web Dup",
            "admin",
            "Administrator"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "rolewebdup",
            "Role Web Dup",
            "ADMIN",
            "Administrator Copy"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Role code already exists in tenant: ADMIN"))
            .andExpect(jsonPath("$.path").value("/api/identity/roles"));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "roleweb02",
            "Role Web 02",
            "operator",
            "Operator",
            "roleweb02@acme.com",
            "Role Operator"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("roleweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ROLEWEB02')].code", hasItem("OPERATOR")));
    }

    @Test
    void returnsNotFoundForMissingTenant() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("missing-roleweb", 0, 10))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Tenant not found: MISSING-ROLEWEB"))
            .andExpect(jsonPath("$.path").value("/api/identity/roles"));
    }

    @Test
    void returnsNotFoundForMissingRoleByTenantAndCode() throws Exception {
        IdentityWebIntegrationTestSupport.createRole(
            mockMvc,
            "roleweb05",
            "Role Web 05",
            "admin",
            "Administrator"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.getRoleRequest("roleweb05", "missing-role"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Role not found for tenant/code: ROLEWEB05/MISSING-ROLE"))
            .andExpect(jsonPath("$.path").value("/api/identity/roles/missing-role"));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("roleweb03", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listRolesRequest("roleweb03", 0, 0))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }
}
