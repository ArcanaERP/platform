package com.arcanaerp.platform.workeffort.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorkEffortsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void changesStatusAndReadsStatusHistory() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb07",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb07",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb07",
            "we-001",
            "IN_PROGRESS",
            "Started picking",
            "AGENT01@WORK.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb07",
            "we-001",
            "IN_PROGRESS",
            "No-op status change",
            "agent01@work.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb07",
                "we-001",
                0,
                10,
                "changedBy", "agent01@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("PLANNED"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("IN_PROGRESS"));
    }

    @Test
    void rejectsUnknownStatusActor() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb08",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb08",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb08",
            "we-001",
            "IN_PROGRESS",
            "Started picking",
            "missing@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort status actor not found in tenant: WORKWEB08/missing@work.com"));
    }

    @Test
    void changesAssignmentAndReadsAssignmentHistory() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "AGENT02@WORK.COM",
            "Coverage handoff",
            "MANAGER@WORK.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedTo").value("agent02@work.com"));

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "agent02@work.com",
            "No-op assignment",
            "manager@work.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedTo").value("agent02@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb09",
                "we-001",
                0,
                10,
                "assignedTo", "agent02@work.com",
                "assignedBy", "manager@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousAssignedTo").value("agent01@work.com"))
            .andExpect(jsonPath("$.items[0].currentAssignedTo").value("agent02@work.com"))
            .andExpect(jsonPath("$.items[0].assignedBy").value("manager@work.com"));
    }

    @Test
    void filtersAssignmentHistoryAtWebBoundary() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent03@work.com",
            "Work Web",
            "Agent 03"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "manager@work.com",
            "Work Web",
            "Manager"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "lead@work.com",
            "Work Web",
            "Lead"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "AGENT02@WORK.COM",
            "Coverage handoff",
            "MANAGER@WORK.COM"
        )
            .andExpect(status().isOk());
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "AGENT03@WORK.COM",
            "Escalation",
            "LEAD@WORK.COM"
        )
            .andExpect(status().isOk());

        MvcResult allHistory = mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest("workweb12", "we-001", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andReturn();
        JsonNode newestAssignment = objectMapper.readTree(allHistory.getResponse().getContentAsString()).path("items").get(0);
        String newestAssignedAt = newestAssignment.path("assignedAt").asText();

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedTo", "AGENT02@WORK.COM"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentAssignedTo").value("agent02@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedBy", "LEAD@WORK.COM"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignedBy").value("lead@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedAtFrom", newestAssignedAt,
                "assignedAtTo", newestAssignedAt
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].currentAssignedTo").value(hasItem(newestAssignment.path("currentAssignedTo").asText())))
            .andExpect(jsonPath("$.items[*].assignedBy").value(hasItem(newestAssignment.path("assignedBy").asText())));
    }

    @Test
    void rejectsUnknownAssignmentAssignee() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb10",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb10",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb10",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb10",
            "we-001",
            "missing@work.com",
            "Coverage handoff",
            "manager@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort assignee not found in tenant: WORKWEB10/missing@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/we-001/assignment"));
    }

    @Test
    void rejectsUnknownAssignmentActor() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb11",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb11",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb11",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb11",
            "we-001",
            "agent02@work.com",
            "Coverage handoff",
            "missing-manager@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort assignment actor not found in tenant: WORKWEB11/missing-manager@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/we-001/assignment"));
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

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedBy", "   "
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedAtFrom", "not-a-timestamp"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedAtFrom", "2026-04-23T00:00:00Z",
                "changedAtTo", "2026-04-22T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "assignedBy", "   "
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedBy query parameter must not be blank"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "assignedAtFrom", "2026-04-23T00:00:00Z",
                "assignedAtTo", "2026-04-22T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedAtFrom must be before or equal to assignedAtTo"));
    }
}
