package com.arcanaerp.platform.agreements.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.StatusHistoryWebTestSupport;
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

        mockMvc.perform(get("/api/agreements/agr-3004"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3004"))
            .andExpect(jsonPath("$.name").value("Read Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundWhenGettingUnknownAgreement() throws Exception {
        mockMvc.perform(get("/api/agreements/agr-missing-read"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-READ"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-read"));
    }

    @Test
    void listsAgreementsWithOptionalStatusFilter() throws Exception {
        String draftPayload = """
            {
              "agreementNumber": "agr-3010",
              "name": "Draft Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activePayload = """
            {
              "agreementNumber": "agr-3011",
              "name": "Active Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String terminatedPayload = """
            {
              "agreementNumber": "agr-3012",
              "name": "Terminated Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activeStatusPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);
        String terminatedStatusPayload = """
            {
              "status": "TERMINATED",
              "tenantCode": "%s",
              "reason": "Mutual termination",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_ALT_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(draftPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activePayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3011/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activeStatusPayload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatedPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_ALT_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3012/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatedStatusPayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/agreements?page=0&size=100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(get("/api/agreements?page=0&size=100&status=ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")));

        mockMvc.perform(get("/api/agreements?page=0&size=100&status=TERMINATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(get("/api/agreements?page=0&size=100&status=DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")));
    }

    @Test
    void listsAgreementStatusHistory() throws Exception {
        String createPayload = """
            {
              "agreementNumber": "agr-3020",
              "name": "History Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activatePayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "LEGAL@ARCANAERP.COM"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3020/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3020/status-history",
            0,
            10
        ))
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
        String createPayload = """
            {
              "agreementNumber": "agr-3021",
              "name": "No-op Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activatePayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "LEGAL@ARCANAERP.COM"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3021/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/agreements/agr-3021/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3021/status-history",
            0,
            10
        ))
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
        String createPayload = """
            {
              "agreementNumber": "agr-3025",
              "name": "Filtered History Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activatePayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "LEGAL@ARCANAERP.COM"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3025/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "tenantCode",
            AGREEMENTS_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE));

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "tenantCode",
            AGREEMENTS_ALT_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "changedBy",
            "LEGAL@ARCANAERP.COM"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "changedBy",
            "ops@arcanaerp.com"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "changedAtFrom",
            "2000-01-01T00:00:00Z",
            "changedAtTo",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
            "changedAtFrom",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void rejectsStatusHistoryFilterWhenTenantCodeBlank() throws Exception {
        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
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
        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
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
        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
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
        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3025/status-history",
            0,
            10,
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
        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-missing-history/status-history",
            0,
            10
        ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-HISTORY"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-history/status-history"));
    }

    @Test
    void rejectsInvalidStatusQueryFilter() throws Exception {
        mockMvc.perform(get("/api/agreements?page=0&size=10&status=invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must be one of: DRAFT, ACTIVE, TERMINATED"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));

        mockMvc.perform(get("/api/agreements?page=0&size=10&status="))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void rejectsStatusTransitionWhenReasonBlank() throws Exception {
        String createPayload = """
            {
              "agreementNumber": "agr-3022",
              "name": "Reason Validation Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String transitionPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "   ",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3022/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transitionPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("reason: must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3022/status"));
    }

    @Test
    void rejectsStatusTransitionWhenActorUnknownInTenant() throws Exception {
        String createPayload = """
            {
              "agreementNumber": "agr-3023",
              "name": "Unknown Actor Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String transitionPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "unknown@arcanaerp.com"
            }
            """.formatted(UNKNOWN_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3023/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transitionPayload))
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
        String createPayload = """
            {
              "agreementNumber": "agr-3024",
              "name": "Mismatched Actor Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String transitionPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "tenant.actor@arcanaerp.com"
            }
            """.formatted(MISMATCH_REQUEST_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(MISMATCH_ACTOR_TENANT_CODE, "tenant.actor@arcanaerp.com");

        mockMvc.perform(patch("/api/agreements/agr-3024/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transitionPayload))
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
        String createPayload = """
            {
              "agreementNumber": "agr-3002",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String statusPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3002/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(statusPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3002"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").value(nullValue()));
    }

    @Test
    void transitionsAgreementStatusFromActiveToTerminated() throws Exception {
        String createPayload = """
            {
              "agreementNumber": "agr-3003",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activatePayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);
        String terminatePayload = """
            {
              "status": "TERMINATED",
              "tenantCode": "%s",
              "reason": "Termination attempt",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        testClock.advance(Duration.ofSeconds(1));
        mockMvc.perform(patch("/api/agreements/agr-3003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").isNotEmpty());

        mockMvc.perform(StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/agreements/agr-3003/status-history",
            0,
            10
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].previousStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"))
            .andExpect(jsonPath("$.items[1].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidAgreementStatusTransitionFromTerminated() throws Exception {
        String createPayload = """
            {
              "agreementNumber": "agr-3006",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;
        String activatePayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);
        String terminatePayload = """
            {
              "status": "TERMINATED",
              "tenantCode": "%s",
              "reason": "Termination attempt",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        mockMvc.perform(patch("/api/agreements/agr-3006/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/agreements/agr-3006/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"));

        mockMvc.perform(patch("/api/agreements/agr-3006/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement status transition not allowed: TERMINATED -> ACTIVE"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3006/status"));
    }

    @Test
    void returnsNotFoundWhenChangingStatusForUnknownAgreement() throws Exception {
        String statusPayload = """
            {
              "status": "ACTIVE",
              "tenantCode": "%s",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """.formatted(AGREEMENTS_TENANT_CODE);

        mockMvc.perform(patch("/api/agreements/agr-missing/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(statusPayload))
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
