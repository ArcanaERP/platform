package com.arcanaerp.platform.agreements.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

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
              "status": "ACTIVE"
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
              "status": "ACTIVE"
            }
            """;
        String terminatePayload = """
            {
              "status": "TERMINATED"
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
              "status": "ACTIVE"
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
