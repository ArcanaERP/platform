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
        CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            "commweb03",
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
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
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
}
