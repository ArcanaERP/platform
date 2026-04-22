package com.arcanaerp.platform.workeffort.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorkEffortsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsWorkEfforts() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb01",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "AGENT01@WORK.COM",
            "2026-04-22T10:00:00Z"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEB01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-001"))
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andExpect(jsonPath("$.assignedTo").value("agent01@work.com"));

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb01",
            "we-002",
            "Confirm receipt",
            "Confirm inbound receipt",
            "IN_PROGRESS",
            "agent02@work.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.getWorkEffortRequest("workweb01", "we-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effortNumber").value("WE-001"))
            .andExpect(jsonPath("$.name").value("Prepare shipment"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest(
                "workweb01",
                0,
                10,
                "status", "PLANNED",
                "assignedTo", "agent01@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.effortNumber=='WE-001')].name", hasItem("Prepare shipment")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb02",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.effortNumber=='WE-001')].name", hasItem("Prepare shipment")));
    }

    @Test
    void rejectsDuplicateTenantLocalEffortNumbers() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb03",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb03",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb03",
            "WE-001",
            "Duplicate effort",
            "Duplicate effort description",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Work effort already exists for tenant/effortNumber: WORKWEB03/WE-001"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts"));
    }

    @Test
    void rejectsUnknownAssignee() throws Exception {
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb04",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "missing@work.com",
            null
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("work effort assignee not found in tenant: WORKWEB04/missing@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts"));
    }

    @Test
    void returnsNotFoundForMissingWorkEffort() throws Exception {
        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.getWorkEffortRequest("workweb05", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Work effort not found for tenant/effortNumber: WORKWEB05/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/missing"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb06", 0, 10, "assignedTo", "   ")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb06", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
