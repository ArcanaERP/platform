package com.arcanaerp.platform.agreements.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.testsupport.web.AgreementManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
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
        return AgreementManagementWebTestSupport.transitionAgreementStatus(
            mockMvc,
            agreementNumber,
            status,
            tenantCode,
            reason,
            changedBy
        );
    }

    static String changeStatusPayload(
        AgreementStatus status,
        String tenantCode,
        String reason,
        String changedBy
    ) {
        return AgreementManagementWebTestSupport.changeStatusPayload(status, tenantCode, reason, changedBy);
    }

    static void registerActor(
        MockMvc mockMvc,
        String tenantCode,
        String email,
        String tenantNamePrefix,
        String displayName
    ) throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            tenantCode,
            email,
            tenantNamePrefix,
            displayName
        );
    }
}
