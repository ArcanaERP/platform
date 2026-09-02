package com.arcanaerp.platform.workeffort.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.arcanaerp.platform.workeffort.WorkEffortDeterministicClockTestSupport;
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
@Import(WorkEffortDeterministicClockTestSupport.Configuration.class)
class WorkEffortsStatusActivitySummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkEffortDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummaries() throws Exception {
        seedTenant("workact01", "agent01@workact.com");
        seedTenant("workact02", "agent02@workact.com");

        createAndTransition("workact01", "we-act-001", "agent01@workact.com", "IN_PROGRESS", "2027-11-01T23:30:00Z");
        createAndTransition("workact01", "we-act-002", "agent01@workact.com", "COMPLETED", "2027-11-02T01:00:00Z");
        createAndTransition("workact02", "we-act-003", "agent02@workact.com", "IN_PROGRESS", "2027-12-02T00:30:00Z");

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "workact01",
            0,
            10,
            "changedAtFrom",
            "2027-11-01T00:00:00Z",
            "changedAtTo",
            "2027-11-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-11-01')].transitionCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-11-02')].workEffortCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-12-02')]").isEmpty());

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.weeklyWorkEffortStatusActivitySummaryRequest(
            "workact01",
            0,
            10,
            "changedAtFrom",
            "2027-11-01T00:00:00Z",
            "changedAtTo",
            "2027-11-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-11-01"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(2))
            .andExpect(jsonPath("$.items[0].workEffortCount").value(2));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.monthlyWorkEffortStatusActivitySummaryRequest(
            "workact01",
            0,
            10,
            "changedAtFrom",
            "2027-11-01T00:00:00Z",
            "changedAtTo",
            "2027-11-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-11"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(2));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "workact01",
            0,
            10,
            "previousStatus",
            "PLANNED",
            "currentStatus",
            "IN_PROGRESS",
            "changedBy",
            "AGENT01@WORKACT.COM",
            "changedAtFrom",
            "2027-11-01T00:00:00Z",
            "changedAtTo",
            "2027-11-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKACT01"))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-11-01"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(1));
    }

    @Test
    void paginatesStatusActivitySummaryBuckets() throws Exception {
        seedTenant("workact03", "agent03@workact.com");

        createAndTransition("workact03", "we-act-010", "agent03@workact.com", "IN_PROGRESS", "2027-12-01T00:00:00Z");
        createAndTransition("workact03", "we-act-011", "agent03@workact.com", "COMPLETED", "2027-12-02T00:00:00Z");

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "workact03",
            0,
            1,
            "changedAtFrom",
            "2027-12-01T00:00:00Z",
            "changedAtTo",
            "2027-12-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-12-02"))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "workact03",
            1,
            1,
            "changedAtFrom",
            "2027-12-01T00:00:00Z",
            "changedAtTo",
            "2027-12-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-12-01"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void rejectsInvalidStatusActivityFilters() throws Exception {
        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "   ",
            0,
            10
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("tenantCode is required"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/status-activity/daily-summary"));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.dailyWorkEffortStatusActivitySummaryRequest(
            "workact04",
            0,
            10,
            "currentStatus",
            "invalid"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("currentStatus query parameter must be one of: PLANNED, IN_PROGRESS, COMPLETED"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/status-activity/daily-summary"));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.weeklyWorkEffortStatusActivitySummaryRequest(
            "workact04",
            0,
            10,
            "changedAtFrom",
            "2027-12-02T00:00:00Z",
            "changedAtTo",
            "2027-12-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/status-activity/weekly-summary"));
    }

    private void seedTenant(String tenantCode, String actorEmail) throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            tenantCode,
            actorEmail,
            "Work Activity",
            "Work Activity Actor"
        );
    }

    private void createAndTransition(
        String tenantCode,
        String effortNumber,
        String actorEmail,
        String targetStatus,
        String changedAt
    ) throws Exception {
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            tenantCode,
            effortNumber,
            "Status Activity Work",
            "Status activity work effort",
            "PLANNED",
            actorEmail,
            null
        )
            .andExpect(status().isCreated());

        testClock.setInstant(Instant.parse(changedAt));
        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            tenantCode,
            effortNumber,
            targetStatus,
            "Status activity transition",
            actorEmail
        )
            .andExpect(status().isOk());
    }
}
