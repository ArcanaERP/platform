package com.arcanaerp.platform.agreements.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgreementsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """;
        String terminatedStatusPayload = """
            {
              "status": "TERMINATED",
              "reason": "Mutual termination",
              "changedBy": "legal@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(draftPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3011/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activeStatusPayload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatedPayload))
            .andExpect(status().isCreated());

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
              "reason": "Initial activation",
              "changedBy": "LEGAL@ARCANAERP.COM"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3020/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/agreements/agr-3020/status-history?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].agreementNumber").value("AGR-3020"))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
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
              "reason": "Initial activation",
              "changedBy": "LEGAL@ARCANAERP.COM"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3021/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/agreements/agr-3021/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/agreements/agr-3021/status-history?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].reason").value("Initial activation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));
    }

    @Test
    void statusHistoryReturnsNotFoundForUnknownAgreement() throws Exception {
        mockMvc.perform(get("/api/agreements/agr-missing-history/status-history?page=0&size=10"))
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
              "reason": "   ",
              "changedBy": "legal@arcanaerp.com"
            }
            """;

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
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

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
    void rejectsInvalidAgreementStatusTransition() throws Exception {
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
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """;
        String terminatePayload = """
            {
              "status": "TERMINATED",
              "reason": "Termination attempt",
              "changedBy": "legal@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/agreements/agr-3003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(activatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/agreements/agr-3003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(terminatePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement status transition not allowed: ACTIVE -> TERMINATED"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3003/status"));
    }

    @Test
    void returnsNotFoundWhenChangingStatusForUnknownAgreement() throws Exception {
        String statusPayload = """
            {
              "status": "ACTIVE",
              "reason": "Initial activation",
              "changedBy": "legal@arcanaerp.com"
            }
            """;

        mockMvc.perform(patch("/api/agreements/agr-missing/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(statusPayload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing/status"));
    }
}
