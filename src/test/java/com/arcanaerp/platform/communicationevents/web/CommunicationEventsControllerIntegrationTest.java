package com.arcanaerp.platform.communicationevents.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
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
class CommunicationEventsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsCommunicationEvents() throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb01", "open", "Open")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, "commweb01", "support", "Support")
            .andExpect(status().isCreated());
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "commweb01",
            "agent01@commweb.com",
            "Comm Web",
            "Agent 01"
        );

        String createdJson = CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            "commweb01",
            "open",
            "support",
            "email",
            "inbound",
            "Support Request",
            "Customer asked for help",
            "2026-04-18T10:15:30Z",
            "AGENT01@COMMWEB.COM",
            "CRM-200"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventNumber", startsWith("COMM-")))
            .andExpect(jsonPath("$.tenantCode").value("COMMWEB01"))
            .andExpect(jsonPath("$.statusCode").value("OPEN"))
            .andExpect(jsonPath("$.purposeCode").value("SUPPORT"))
            .andExpect(jsonPath("$.channel").value("EMAIL"))
            .andExpect(jsonPath("$.direction").value("INBOUND"))
            .andExpect(jsonPath("$.recordedBy").value("agent01@commweb.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String eventNumber = CommunicationEventsWebIntegrationTestSupport.extractJsonString(createdJson, "eventNumber");

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.getEventRequest("commweb01", eventNumber))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventNumber").value(eventNumber))
            .andExpect(jsonPath("$.subject").value("Support Request"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.listEventsRequest(
                "commweb01",
                0,
                10,
                "statusCode", "open",
                "purposeCode", "support",
                "channel", "email",
                "direction", "inbound",
                "recordedBy", "agent01@commweb.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.eventNumber=='" + eventNumber + "')].subject", hasItem("Support Request")));
    }

    @Test
    void returnsNotFoundForMissingCommunicationEvent() throws Exception {
        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.getEventRequest("commweb02", "COMM-MISSING"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Communication event not found for tenant/eventNumber: COMMWEB02/COMM-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/communication-events/COMM-MISSING"));
    }

    @Test
    void rejectsUnknownRecordedByActor() throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb03", "open", "Open")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, "commweb03", "support", "Support")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            "commweb03",
            "open",
            "support",
            "phone",
            "outbound",
            "Follow-up Call",
            "Attempted outreach",
            "2026-04-18T10:15:30Z",
            "missing@commweb.com",
            null
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("recordedBy actor not found in tenant: COMMWEB03/missing@commweb.com"))
            .andExpect(jsonPath("$.path").value("/api/communication-events"));
    }

    @Test
    void rejectsInvalidListFilters() throws Exception {
        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.listEventsRequest("commweb04", 0, 10, "channel", "")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("channel query parameter must not be blank"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.listEventsRequest("commweb04", 0, 10, "direction", "sideways")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("direction query parameter is invalid"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.listEventsRequest("commweb04", 0, 10, "statusCode", "")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("statusCode query parameter must not be blank"));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb05", "open", "Open")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, "commweb05", "internal_note", "Internal Note")
            .andExpect(status().isCreated());
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "commweb05",
            "agent05@commweb.com",
            "Comm Web",
            "Agent 05"
        );

        CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            "commweb05",
            "open",
            "internal_note",
            "note",
            "internal",
            "Case Note",
            "Internal review note",
            "2026-04-18T10:15:30Z",
            "agent05@commweb.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.listEventsRequest("commweb05"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)));
    }

    @Test
    void createsAndListsStatusAndPurposeTypes() throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb06", "open", "Open")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("OPEN"));
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, "commweb06", "support", "Support")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("SUPPORT"));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.listStatusTypesRequest("commweb06", 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.code=='OPEN')].name", hasItem("Open")));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.listPurposeTypesRequest("commweb06", 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.code=='SUPPORT')].name", hasItem("Support")));
    }

    @Test
    void changesStatusAndListsStatusHistory() throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb07", "open", "Open")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, "commweb07", "closed", "Closed")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, "commweb07", "support", "Support")
            .andExpect(status().isCreated());
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "commweb07",
            "agent07@commweb.com",
            "Comm Web",
            "Agent 07"
        );

        String createdJson = CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            "commweb07",
            "open",
            "support",
            "email",
            "inbound",
            "Support Request",
            "Customer asked for help",
            "2026-04-18T10:15:30Z",
            "agent07@commweb.com",
            null
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String eventNumber = CommunicationEventsWebIntegrationTestSupport.extractJsonString(createdJson, "eventNumber");

        CommunicationEventsWebIntegrationTestSupport.changeStatus(
            mockMvc,
            "commweb07",
            eventNumber,
            "closed",
            "Resolved by support",
            "AGENT07@COMMWEB.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value("CLOSED"))
            .andExpect(jsonPath("$.statusName").value("Closed"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.statusHistoryRequest(
                "commweb07",
                eventNumber,
                0,
                10,
                "changedBy", "agent07@commweb.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].eventNumber").value(eventNumber))
            .andExpect(jsonPath("$.items[0].previousStatusCode").value("OPEN"))
            .andExpect(jsonPath("$.items[0].currentStatusCode").value("CLOSED"))
            .andExpect(jsonPath("$.items[0].reason").value("Resolved by support"))
            .andExpect(jsonPath("$.items[0].changedBy").value("agent07@commweb.com"));
    }

    @Test
    void rejectsInvalidStatusHistoryFilters() throws Exception {
        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.statusHistoryRequest(
                "commweb08",
                "COMM-TEST",
                0,
                10,
                "changedBy", ""
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedBy query parameter must not be blank"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.statusHistoryRequest(
                "commweb08",
                "COMM-TEST",
                0,
                10,
                "changedAtFrom", "not-a-timestamp"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(
            CommunicationEventsWebIntegrationTestSupport.statusHistoryRequest(
                "commweb08",
                "COMM-TEST",
                0,
                10,
                "changedAtFrom", "2026-04-19T00:00:00Z",
                "changedAtTo", "2026-04-18T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"));
    }
}
