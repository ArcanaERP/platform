package com.arcanaerp.platform.agreements.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

final class AgreementsIntegrationTestSupport {

    private AgreementsIntegrationTestSupport() {}

    static void createAgreement(MockMvc mockMvc, String agreementNumber, String name) throws Exception {
        mockMvc.perform(
            post("/api/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAgreementPayload(agreementNumber, name))
        )
            .andExpect(status().isCreated());
    }

    static String createAgreementPayload(String agreementNumber, String name) {
        return """
            {
              "agreementNumber": "%s",
              "name": "%s",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """.formatted(agreementNumber, name);
    }

    static ResultActions transitionAgreementStatus(
        MockMvc mockMvc,
        String agreementNumber,
        AgreementStatus status,
        String tenantCode,
        String reason,
        String changedBy
    ) throws Exception {
        return mockMvc.perform(
            patch("/api/agreements/" + agreementNumber + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeStatusPayload(status, tenantCode, reason, changedBy))
        );
    }

    static String changeStatusPayload(
        AgreementStatus status,
        String tenantCode,
        String reason,
        String changedBy
    ) {
        return """
            {
              "status": "%s",
              "tenantCode": "%s",
              "reason": "%s",
              "changedBy": "%s"
            }
            """.formatted(status.name(), tenantCode, reason, changedBy);
    }

    static void registerActor(
        MockMvc mockMvc,
        String tenantCode,
        String email,
        String tenantNamePrefix,
        String displayName
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "%s %s",
              "roleCode": "OPS",
              "roleName": "Operations",
              "email": "%s",
              "displayName": "%s"
            }
            """.formatted(tenantCode, tenantNamePrefix, tenantCode, email, displayName);

        var result = mockMvc.perform(post("/api/identity/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andReturn();
        int statusCode = result.getResponse().getStatus();
        if (statusCode == 400) {
            assertThat(result.getResponse().getContentAsString()).contains("User email already exists in tenant");
            return;
        }
        if (statusCode != 201) {
            throw new AssertionError("Unexpected status while registering actor: " + statusCode);
        }
    }
}
