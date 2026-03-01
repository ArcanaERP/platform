package com.arcanaerp.platform.agreements.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AgreementsLifecycleTransitionMatrixIntegrationTest {

    private static final String TENANT_CODE = "TENAGRMX";
    private static final String CHANGED_BY = "matrix@arcanaerp.com";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{index}: {0} -> {1} allowed={2}")
    @MethodSource("transitionCases")
    void enforcesLifecycleTransitionMatrix(AgreementStatus fromStatus, AgreementStatus targetStatus, boolean allowed)
        throws Exception {
        registerActor(TENANT_CODE, CHANGED_BY);

        String agreementNumber = "agr-mtx-" + fromStatus.name().toLowerCase() + "-" + targetStatus.name().toLowerCase();
        createAgreement(agreementNumber);
        transitionToInitialState(agreementNumber, fromStatus);

        ResultActions transitionResult = transition(agreementNumber, targetStatus, "Matrix transition");
        if (allowed) {
            transitionResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementNumber").value(agreementNumber.toUpperCase()))
                .andExpect(jsonPath("$.status").value(targetStatus.name()));
            return;
        }

        transitionResult
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Agreement status transition not allowed: " + fromStatus.name() + " -> " + targetStatus.name()))
            .andExpect(jsonPath("$.path").value("/api/agreements/" + agreementNumber + "/status"));
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(
            Arguments.of(AgreementStatus.DRAFT, AgreementStatus.DRAFT, true),
            Arguments.of(AgreementStatus.DRAFT, AgreementStatus.ACTIVE, true),
            Arguments.of(AgreementStatus.DRAFT, AgreementStatus.TERMINATED, true),
            Arguments.of(AgreementStatus.ACTIVE, AgreementStatus.DRAFT, false),
            Arguments.of(AgreementStatus.ACTIVE, AgreementStatus.ACTIVE, true),
            Arguments.of(AgreementStatus.ACTIVE, AgreementStatus.TERMINATED, true),
            Arguments.of(AgreementStatus.TERMINATED, AgreementStatus.DRAFT, false),
            Arguments.of(AgreementStatus.TERMINATED, AgreementStatus.ACTIVE, false),
            Arguments.of(AgreementStatus.TERMINATED, AgreementStatus.TERMINATED, true)
        );
    }

    private void createAgreement(String agreementNumber) throws Exception {
        String payload = """
            {
              "agreementNumber": "%s",
              "name": "Matrix Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """.formatted(agreementNumber);

        mockMvc.perform(post("/api/agreements").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }

    private void transitionToInitialState(String agreementNumber, AgreementStatus initialStatus) throws Exception {
        if (initialStatus == AgreementStatus.DRAFT) {
            return;
        }

        transition(agreementNumber, initialStatus, "Matrix setup")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(initialStatus.name()));
    }

    private ResultActions transition(String agreementNumber, AgreementStatus status, String reason) throws Exception {
        String payload = """
            {
              "status": "%s",
              "tenantCode": "%s",
              "reason": "%s",
              "changedBy": "%s"
            }
            """.formatted(status.name(), TENANT_CODE, reason, CHANGED_BY);

        return mockMvc.perform(
            patch("/api/agreements/" + agreementNumber + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        );
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "Agreements Matrix Tenant %s",
              "roleCode": "OPS",
              "roleName": "Operations",
              "email": "%s",
              "displayName": "Agreements Matrix Actor"
            }
            """.formatted(tenantCode, tenantCode, email);

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
