package com.arcanaerp.platform.communicationevents.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CommunicationEventsDeterministicClockTestSupport.Configuration.class)
class CommunicationEventsStatusActivitySummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunicationEventsDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummaries() throws Exception {
        seedTenant("commact01", "agent01@commact.com");
        seedTenant("commact02", "agent02@commact.com");

        createAndTransition("commact01", "agent01@commact.com", "closed", "2027-07-01T23:30:00Z");
        createAndTransition("commact01", "agent01@commact.com", "escalated", "2027-07-02T01:00:00Z");
        createAndTransition("commact02", "agent02@commact.com", "closed", "2027-08-02T00:30:00Z");

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "commact01",
            0,
            10,
            "changedAtFrom",
            "2027-07-01T00:00:00Z",
            "changedAtTo",
            "2027-07-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-07-01')].transitionCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-07-02')].eventCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-08-02')]").isEmpty());

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
            "commact01",
            0,
            10,
            "changedAtFrom",
            "2027-07-01T00:00:00Z",
            "changedAtTo",
            "2027-07-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-06-28"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(2))
            .andExpect(jsonPath("$.items[0].eventCount").value(2));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.monthlyStatusActivitySummaryRequest(
            "commact01",
            0,
            10,
            "changedAtFrom",
            "2027-07-01T00:00:00Z",
            "changedAtTo",
            "2027-07-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-07"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(2));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "commact01",
            0,
            10,
            "previousStatusCode",
            "open",
            "currentStatusCode",
            "closed",
            "changedBy",
            "AGENT01@COMMACT.COM",
            "changedAtFrom",
            "2027-07-01T00:00:00Z",
            "changedAtTo",
            "2027-07-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-07-01"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(1));
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummariesByCurrentStatusCode() throws Exception {
        seedTenant("commact05", "agent05@commact.com");
        seedTenant("commact06", "agent06@commact.com");

        createAndTransition("commact05", "agent05@commact.com", "closed", "2027-10-01T00:00:00Z");
        createAndTransition("commact05", "agent05@commact.com", "escalated", "2027-10-01T01:00:00Z");
        createAndTransition("commact06", "agent06@commact.com", "closed", "2027-10-08T00:00:00Z");

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivityByCurrentStatusSummaryRequest(
            "commact05",
            0,
            10,
            "changedAtFrom",
            "2027-10-01T00:00:00Z",
            "changedAtTo",
            "2027-10-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-10-01' && @.currentStatusCode=='CLOSED')].currentStatusName",
                hasItem("Closed")))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-10-01' && @.currentStatusCode=='ESCALATED')].transitionCount",
                hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-10-08')]").isEmpty());

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.weeklyStatusActivityByCurrentStatusSummaryRequest(
            "commact05",
            0,
            10,
            "changedAtFrom",
            "2027-10-01T00:00:00Z",
            "changedAtTo",
            "2027-10-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessWeekStart=='2027-09-27' && @.currentStatusCode=='CLOSED')].eventCount",
                hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessWeekStart=='2027-09-27' && @.currentStatusCode=='ESCALATED')].eventCount",
                hasItem(1)));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.monthlyStatusActivityByCurrentStatusSummaryRequest(
            "commact05",
            0,
            10,
            "changedAtFrom",
            "2027-10-01T00:00:00Z",
            "changedAtTo",
            "2027-10-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-10"))
            .andExpect(jsonPath("$.items[0].currentStatusCode").value("CLOSED"))
            .andExpect(jsonPath("$.items[1].currentStatusCode").value("ESCALATED"));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivityByCurrentStatusSummaryRequest(
            "commact05",
            0,
            1,
            "changedAtFrom",
            "2027-10-01T00:00:00Z",
            "changedAtTo",
            "2027-10-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].currentStatusCode").value("CLOSED"))
            .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivityByCurrentStatusSummaryRequest(
            "commact05",
            0,
            10,
            "currentStatusCode",
            "closed",
            "changedAtFrom",
            "2027-10-01T00:00:00Z",
            "changedAtTo",
            "2027-10-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentStatusCode").value("CLOSED"));
    }

    @Test
    void paginatesStatusActivitySummaryBuckets() throws Exception {
        seedTenant("commact03", "agent03@commact.com");

        createAndTransition("commact03", "agent03@commact.com", "closed", "2027-09-01T00:00:00Z");
        createAndTransition("commact03", "agent03@commact.com", "escalated", "2027-09-02T00:00:00Z");

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "commact03",
            0,
            1,
            "changedAtFrom",
            "2027-09-01T00:00:00Z",
            "changedAtTo",
            "2027-09-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-09-02"))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "commact03",
            1,
            1,
            "changedAtFrom",
            "2027-09-01T00:00:00Z",
            "changedAtTo",
            "2027-09-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-09-01"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void rejectsInvalidStatusActivityFilters() throws Exception {
        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "   ",
            0,
            10
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("tenantCode is required"))
            .andExpect(jsonPath("$.path").value("/api/communication-events/status-activity/daily-summary"));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            "commact04",
            0,
            10,
            "previousStatusCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("previousStatusCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/communication-events/status-activity/daily-summary"));

        mockMvc.perform(CommunicationEventsWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
            "commact04",
            0,
            10,
            "changedAtFrom",
            "2027-09-02T00:00:00Z",
            "changedAtTo",
            "2027-09-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/communication-events/status-activity/weekly-summary"));
    }

    private void seedTenant(String tenantCode, String actorEmail) throws Exception {
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, tenantCode, "open", "Open")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, tenantCode, "closed", "Closed")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createStatusType(mockMvc, tenantCode, "escalated", "Escalated")
            .andExpect(status().isCreated());
        CommunicationEventsWebIntegrationTestSupport.createPurposeType(mockMvc, tenantCode, "support", "Support")
            .andExpect(status().isCreated());
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            tenantCode,
            actorEmail,
            "Communication Activity",
            "Communication Activity Actor"
        );
    }

    private void createAndTransition(String tenantCode, String actorEmail, String statusCode, String changedAt) throws Exception {
        String createdJson = CommunicationEventsWebIntegrationTestSupport.createEvent(
            mockMvc,
            tenantCode,
            "open",
            "support",
            "email",
            "inbound",
            "Status Activity Event",
            "Status activity test event",
            "2027-07-01T00:00:00Z",
            actorEmail,
            null
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String eventNumber = CommunicationEventsWebIntegrationTestSupport.extractJsonString(createdJson, "eventNumber");

        testClock.setInstant(Instant.parse(changedAt));
        CommunicationEventsWebIntegrationTestSupport.changeStatus(
            mockMvc,
            tenantCode,
            eventNumber,
            statusCode,
            "Status activity transition",
            actorEmail
        )
            .andExpect(status().isOk());
    }
}
