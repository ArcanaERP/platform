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
    void readsUserById() throws Exception {
        String createdUserJson = IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme06",
            "Acme 06",
            "admin",
            "Administrator",
            "ops06@acme.com",
            "Ops 06"
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String userId = IdentityWebIntegrationTestSupport.extractJsonString(createdUserJson, "id");

        mockMvc.perform(IdentityWebIntegrationTestSupport.getUserRequest(userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.tenantCode").value("ACME06"))
            .andExpect(jsonPath("$.roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("ops06@acme.com"));
    }

    @Test
    void updatesUserDisplayNameAndActiveState() throws Exception {
        String createdUserJson = IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme07",
            "Acme 07",
            "admin",
            "Administrator",
            "ops07@acme.com",
            "Ops 07"
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String userId = IdentityWebIntegrationTestSupport.extractJsonString(createdUserJson, "id");

        IdentityWebIntegrationTestSupport.updateUser(
            mockMvc,
            userId,
            "Ops 07 Renamed",
            false
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.displayName").value("Ops 07 Renamed"))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void returnsNotFoundForMissingUserById() throws Exception {
        String missingUserId = "11111111-1111-1111-1111-111111111111";

        mockMvc.perform(IdentityWebIntegrationTestSupport.getUserRequest(missingUserId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User not found: " + missingUserId))
            .andExpect(jsonPath("$.path").value("/api/identity/users/" + missingUserId));
    }

    @Test
    void returnsNotFoundForMissingUserUpdate() throws Exception {
        String missingUserId = "11111111-1111-1111-1111-111111111111";

        IdentityWebIntegrationTestSupport.updateUser(
            mockMvc,
            missingUserId,
            "Missing User",
            false
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User not found: " + missingUserId))
            .andExpect(jsonPath("$.path").value("/api/identity/users/" + missingUserId));
    }

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
    void rejectsInvalidUserIdLookup() throws Exception {
        mockMvc.perform(IdentityWebIntegrationTestSupport.getUserRequest("not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("userId is invalid"))
            .andExpect(jsonPath("$.path").value("/api/identity/users/not-a-uuid"));
    }

    @Test
    void rejectsInvalidUserIdUpdate() throws Exception {
        IdentityWebIntegrationTestSupport.updateUser(
            mockMvc,
            "not-a-uuid",
            "Invalid User",
            false
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("userId is invalid"))
            .andExpect(jsonPath("$.path").value("/api/identity/users/not-a-uuid"));
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

        IdentityWebIntegrationTestSupport.createUser(
            mockMvc,
            "acme05",
            "Acme 05",
            "analyst",
            "Analyst",
            "ops05@acme.com",
            "Ops 05"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(0, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(IdentityWebIntegrationTestSupport.listUsersRequest(1, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.hasPrevious").value(true));
    }
}
