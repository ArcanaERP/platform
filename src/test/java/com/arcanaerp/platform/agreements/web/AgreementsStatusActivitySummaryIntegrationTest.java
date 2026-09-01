package com.arcanaerp.platform.agreements.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
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
@Import(AgreementsDeterministicClockTestSupport.Configuration.class)
class AgreementsStatusActivitySummaryIntegrationTest {

    private static final String TENANT_A = "TENAGRSA";
    private static final String TENANT_B = "TENAGRSB";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgreementsDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() throws Exception {
        testClock.resetToBaseInstant();
        AgreementsWebIntegrationTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            TENANT_A,
            "legal.summary@arcanaerp.com",
            "Agreement Summary Tenant",
            "Legal Summary"
        );
        AgreementsWebIntegrationTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            TENANT_B,
            "ops.summary@arcanaerp.com",
            "Agreement Summary Tenant",
            "Ops Summary"
        );
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummaries() throws Exception {
        createAndTransition("agr-3400", TENANT_A, "Tenant A First", AgreementStatus.ACTIVE, "legal.summary@arcanaerp.com",
            "2027-03-01T23:30:00Z");
        createAndTransition("agr-3401", TENANT_A, "Tenant A Second", AgreementStatus.ACTIVE, "legal.summary@arcanaerp.com",
            "2027-03-02T01:00:00Z");
        createAndTransition("agr-3402", TENANT_B, "Tenant B First", AgreementStatus.TERMINATED, "ops.summary@arcanaerp.com",
            "2027-04-01T00:30:00Z");

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            10,
            "changedAtFrom",
            "2027-01-01T00:00:00Z",
            "changedAtTo",
            "2027-04-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-03-01')].transitionCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-03-02')].agreementCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-04-01')].transitionCount", hasItem(1)));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
            0,
            10,
            "changedAtFrom",
            "2027-01-01T00:00:00Z",
            "changedAtTo",
            "2027-04-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessWeekStart=='2027-03-01')].transitionCount", hasItem(2)))
            .andExpect(jsonPath("$.items[?(@.businessWeekStart=='2027-03-29')].transitionCount", hasItem(1)));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.monthlyStatusActivitySummaryRequest(
            0,
            10,
            "changedAtFrom",
            "2027-01-01T00:00:00Z",
            "changedAtTo",
            "2027-04-30T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessMonth=='2027-03')].transitionCount", hasItem(2)))
            .andExpect(jsonPath("$.items[?(@.businessMonth=='2027-03')].agreementCount", hasItem(2)))
            .andExpect(jsonPath("$.items[?(@.businessMonth=='2027-04')].transitionCount", hasItem(1)));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            10,
            "tenantCode",
            TENANT_A,
            "currentStatus",
            "ACTIVE",
            "changedBy",
            "LEGAL.SUMMARY@ARCANAERP.COM",
            "changedAtFrom",
            "2027-03-01T00:00:00Z",
            "changedAtTo",
            "2027-03-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-03-01')].transitionCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-03-02')].transitionCount", hasItem(1)))
            .andExpect(jsonPath("$.items[?(@.businessDate=='2027-04-01')]").isEmpty());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            10,
            "previousStatus",
            "ACTIVE",
            "changedAtFrom",
            "2027-01-01T00:00:00Z",
            "changedAtTo",
            "2027-12-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void paginatesStatusActivitySummaryBuckets() throws Exception {
        createAndTransition("agr-3410", TENANT_A, "Tenant A Page One", AgreementStatus.ACTIVE, "legal.summary@arcanaerp.com",
            "2027-05-01T00:00:00Z");
        createAndTransition("agr-3411", TENANT_A, "Tenant A Page Two", AgreementStatus.ACTIVE, "legal.summary@arcanaerp.com",
            "2027-05-02T00:00:00Z");

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            1,
            "changedAtFrom",
            "2027-05-01T00:00:00Z",
            "changedAtTo",
            "2027-05-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-05-02"))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            1,
            1,
            "changedAtFrom",
            "2027-05-01T00:00:00Z",
            "changedAtTo",
            "2027-05-31T23:59:59Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-05-01"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void rejectsInvalidStatusActivityFilters() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            10,
            "tenantCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("tenantCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/status-activity/daily-summary"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
            0,
            10,
            "currentStatus",
            "invalid"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("currentStatus query parameter must be one of: DRAFT, ACTIVE, TERMINATED"))
            .andExpect(jsonPath("$.path").value("/api/agreements/status-activity/daily-summary"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
            0,
            10,
            "changedAtFrom",
            "2026-03-02T00:00:00Z",
            "changedAtTo",
            "2026-03-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/agreements/status-activity/weekly-summary"));
    }

    private void createAndTransition(
        String agreementNumber,
        String tenantCode,
        String name,
        AgreementStatus targetStatus,
        String changedBy,
        String changedAt
    ) throws Exception {
        AgreementsWebIntegrationTestSupport.createAgreement(mockMvc, tenantCode, agreementNumber, name)
            .andExpect(status().isCreated());
        testClock.setInstant(Instant.parse(changedAt));
        AgreementsWebIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            agreementNumber,
            targetStatus,
            tenantCode,
            "Lifecycle summary transition",
            changedBy
        )
            .andExpect(status().isOk());
    }
}
