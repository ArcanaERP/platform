package com.arcanaerp.platform.agreements.web;

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
        AgreementsIntegrationTestSupport.registerActor(
            mockMvc,
            TENANT_CODE,
            CHANGED_BY,
            "Agreements Matrix Tenant",
            "Agreements Matrix Actor"
        );

        String agreementNumber = "agr-mtx-" + fromStatus.name().toLowerCase() + "-" + targetStatus.name().toLowerCase();
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, agreementNumber, "Matrix Agreement");
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

    private void transitionToInitialState(String agreementNumber, AgreementStatus initialStatus) throws Exception {
        if (initialStatus == AgreementStatus.DRAFT) {
            return;
        }

        transition(agreementNumber, initialStatus, "Matrix setup")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(initialStatus.name()));
    }

    private ResultActions transition(String agreementNumber, AgreementStatus status, String reason) throws Exception {
        return AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            agreementNumber,
            status,
            TENANT_CODE,
            reason,
            CHANGED_BY
        );
    }
}
