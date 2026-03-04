package com.arcanaerp.platform.agreements.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.testsupport.web.AgreementCatalogWebTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AgreementsDeterministicClockTestSupport.Configuration.class)
class AgreementsControllerIntegrationTest {

    private static final String AGREEMENTS_TENANT_CODE = "TENAGR01";
    private static final String AGREEMENTS_ALT_TENANT_CODE = "TENAGR02";
    private static final String UNKNOWN_TENANT_CODE = "TENAGR99";
    private static final String MISMATCH_ACTOR_TENANT_CODE = "TENAGR11";
    private static final String MISMATCH_REQUEST_TENANT_CODE = "TENAGR12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgreementsDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsAgreement() throws Exception {
        String payload = """
            {
              "agreementNumber": " agr-3000 ",
              "name": "  Master Services Agreement ",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3000"))
            .andExpect(jsonPath("$.name").value("Master Services Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.activatedAt").value(nullValue()))
            .andExpect(jsonPath("$.terminatedAt").value(nullValue()))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void returnsErrorEnvelopeForDuplicateAgreementNumber() throws Exception {
        String payload = """
            {
              "agreementNumber": "agr-3001",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement number already exists: AGR-3001"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void getsAgreementByAgreementNumber() throws Exception {
        String payload = """
            {
              "agreementNumber": "agr-3004",
              "name": "Read Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(AgreementCatalogWebTestSupport.getAgreementRequest("agr-3004"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3004"))
            .andExpect(jsonPath("$.name").value("Read Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundWhenGettingUnknownAgreement() throws Exception {
        mockMvc.perform(AgreementCatalogWebTestSupport.getAgreementRequest("agr-missing-read"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-READ"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-read"));
    }

    @Test
    void listsAgreementsWithOptionalStatusFilter() throws Exception {

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3010", "Draft Agreement");

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3011", "Active Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3011",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk());

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3012", "Terminated Agreement");

        registerActor(AGREEMENTS_ALT_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3012",
            AgreementStatus.TERMINATED,
            AGREEMENTS_ALT_TENANT_CODE,
            "Mutual termination",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 100, "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")));

        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 100, "TERMINATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 100, "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")));
    }

    @Test
    void listsAgreementStatusHistory() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3020", "History Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3020",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3020"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].agreementNumber").value("AGR-3020"))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE))
            .andExpect(jsonPath("$.items[0].reason").value("Initial activation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void statusHistoryIgnoresNoOpTransitions() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3021", "No-op Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3021",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3021",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3021"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE))
            .andExpect(jsonPath("$.items[0].reason").value("Initial activation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));
    }

    @Test
    void filtersStatusHistoryByTenantAndChangedByAndChangedAtRange() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3025", "Filtered History Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3025",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            AGREEMENTS_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            AGREEMENTS_ALT_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "LEGAL@ARCANAERP.COM"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "ops@arcanaerp.com"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2000-01-01T00:00:00Z",
            "changedAtTo",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void rejectsStatusHistoryFilterWhenTenantCodeBlank() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("tenantCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedByBlank() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedBy query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedAtFromInvalid() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "not-a-timestamp"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedAtRangeInvalid() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2026-03-02T00:00:00Z",
            "changedAtTo",
            "2026-03-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void statusHistoryReturnsNotFoundForUnknownAgreement() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-missing-history"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-HISTORY"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-history/status-history"));
    }

    @Test
    void rejectsInvalidStatusQueryFilter() throws Exception {
        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 10, "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must be one of: DRAFT, ACTIVE, TERMINATED"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));

        mockMvc.perform(AgreementCatalogWebTestSupport.listAgreementsRequest(0, 10, ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void rejectsStatusTransitionWhenReasonBlank() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3022", "Reason Validation Agreement");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3022",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "   ",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("reason: must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3022/status"));
    }

    @Test
    void rejectsStatusTransitionWhenActorUnknownInTenant() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3023", "Unknown Actor Agreement");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3023",
            AgreementStatus.ACTIVE,
            UNKNOWN_TENANT_CODE,
            "Initial activation",
            "unknown@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Agreement status actor not found in tenant "
                    + UNKNOWN_TENANT_CODE
                    + ": unknown@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3023/status"));
    }

    @Test
    void rejectsStatusTransitionWhenActorExistsInDifferentTenant() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3024", "Mismatched Actor Agreement");

        registerActor(MISMATCH_ACTOR_TENANT_CODE, "tenant.actor@arcanaerp.com");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3024",
            AgreementStatus.ACTIVE,
            MISMATCH_REQUEST_TENANT_CODE,
            "Initial activation",
            "tenant.actor@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Agreement status actor not found in tenant "
                    + MISMATCH_REQUEST_TENANT_CODE
                    + ": tenant.actor@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3024/status"));
    }

    @Test
    void transitionsAgreementStatusFromDraftToActive() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3002", "Master Services Agreement")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3002",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3002"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").value(nullValue()));
    }

    @Test
    void transitionsAgreementStatusFromActiveToTerminated() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3003", "Master Services Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3003",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        testClock.advance(Duration.ofSeconds(1));
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3003",
            AgreementStatus.TERMINATED,
            AGREEMENTS_TENANT_CODE,
            "Termination attempt",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").isNotEmpty());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].previousStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"))
            .andExpect(jsonPath("$.items[1].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidAgreementStatusTransitionFromTerminated() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3006", "Master Services Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.TERMINATED,
            AGREEMENTS_TENANT_CODE,
            "Termination attempt",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"));

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement status transition not allowed: TERMINATED -> ACTIVE"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3006/status"));
    }

    @Test
    void returnsNotFoundWhenChangingStatusForUnknownAgreement() throws Exception {
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-missing",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing/status"));
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        AgreementsIntegrationTestSupport.registerActor(
            mockMvc,
            tenantCode,
            email,
            "Agreements Tenant",
            "Agreements Actor"
        );
    }
}
