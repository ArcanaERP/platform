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
class UsersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsUser() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme01",
            "Acme 01",
            "admin",
            "Administrator",
            "OPS01@ACME.COM",
            "Ops User"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("ACME01"))
            .andExpect(jsonPath("$.roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("ops01@acme.com"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ACME01')].displayName", hasItem("Ops User")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme03",
            "Acme 03",
            "operator",
            "Operator",
            "ops03@acme.com",
            "Ops 03"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.tenantCode=='ACME03')].displayName", hasItem("Ops 03")));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateTenantEmail() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme02",
            "Acme 02",
            "admin",
            "Administrator",
            "ops02@acme.com",
            "Ops User"
        )
            .andExpect(status().isCreated());

        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme02",
            "Acme 02",
            "admin",
            "Administrator",
            "ops02@acme.com",
            "Ops User"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/identity/users"));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(-1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"))
            .andExpect(jsonPath("$.path").value("/api/identity/users"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(0, 0))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"))
            .andExpect(jsonPath("$.path").value("/api/identity/users"));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(0, 101))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"))
            .andExpect(jsonPath("$.path").value("/api/identity/users"));
    }

    @Test
    void paginatesUsersAtPageBoundaries() throws Exception {
        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme04",
            "Acme 04",
            "analyst",
            "Analyst",
            "ops04@acme.com",
            "Ops 04"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(0, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(1, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1));
    }
}
