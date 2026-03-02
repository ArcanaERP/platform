package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.arcanaerp.platform.agreements.AgreementStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class AgreementManagementWebTestSupport {

    private AgreementManagementWebTestSupport() {}

    public static ResultActions transitionAgreementStatus(
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

    public static String changeStatusPayload(
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
}
