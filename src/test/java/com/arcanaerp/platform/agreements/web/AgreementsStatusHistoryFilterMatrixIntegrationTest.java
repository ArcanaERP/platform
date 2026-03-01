package com.arcanaerp.platform.agreements.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class AgreementsStatusHistoryFilterMatrixIntegrationTest {

    private static final String TENANT_ALPHA = "TENHFM01";
    private static final String TENANT_BETA = "TENHFM02";
    private static final String TENANT_UNKNOWN = "TENHFM99";
    private static final String ACTOR_ALPHA = "legal.filters@arcanaerp.com";
    private static final String ACTOR_BETA = "ops.filters@arcanaerp.com";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validFilterCases")
    void filtersStatusHistoryWithMatrixCombinations(String caseId, HistoryFilterCase filterCase) throws Exception {
        String agreementNumber = "agr-hfmx-" + caseId;
        seedAgreementStatusHistory(agreementNumber);

        ResultActions result = mockMvc.perform(statusHistoryRequest(
            agreementNumber,
            filterCase.tenantCode(),
            filterCase.changedBy(),
            filterCase.changedAtFrom(),
            filterCase.changedAtTo()
        ));

        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(filterCase.expectedTotalItems()));

        if (filterCase.expectedTenantCode() != null) {
            result.andExpect(jsonPath("$.items[0].tenantCode").value(filterCase.expectedTenantCode()));
        }
        if (filterCase.expectedChangedBy() != null) {
            result.andExpect(jsonPath("$.items[0].changedBy").value(filterCase.expectedChangedBy()));
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidFilterCases")
    void rejectsInvalidStatusHistoryFilterParameters(String caseId, InvalidFilterCase invalidCase) throws Exception {
        String agreementNumber = "agr-hfmx-invalid";

        mockMvc.perform(statusHistoryRequest(
            agreementNumber,
            invalidCase.tenantCode(),
            invalidCase.changedBy(),
            invalidCase.changedAtFrom(),
            invalidCase.changedAtTo()
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(invalidCase.expectedMessage()))
            .andExpect(jsonPath("$.path").value("/api/agreements/" + agreementNumber + "/status-history"));
    }

    private static Stream<Arguments> validFilterCases() {
        return Stream.of(
            Arguments.of(
                "no-filters",
                new HistoryFilterCase(null, null, null, null, 2, null, null)
            ),
            Arguments.of(
                "tenant-alpha",
                new HistoryFilterCase(TENANT_ALPHA, null, null, null, 1, TENANT_ALPHA, null)
            ),
            Arguments.of(
                "tenant-beta",
                new HistoryFilterCase(TENANT_BETA, null, null, null, 1, TENANT_BETA, null)
            ),
            Arguments.of(
                "tenant-unknown",
                new HistoryFilterCase(TENANT_UNKNOWN, null, null, null, 0, null, null)
            ),
            Arguments.of(
                "changed-by-alpha-uppercase",
                new HistoryFilterCase(null, ACTOR_ALPHA.toUpperCase(), null, null, 1, TENANT_ALPHA, ACTOR_ALPHA)
            ),
            Arguments.of(
                "changed-by-beta",
                new HistoryFilterCase(null, ACTOR_BETA, null, null, 1, TENANT_BETA, ACTOR_BETA)
            ),
            Arguments.of(
                "changed-by-unknown",
                new HistoryFilterCase(null, "unknown.filters@arcanaerp.com", null, null, 0, null, null)
            ),
            Arguments.of(
                "tenant-and-actor-match",
                new HistoryFilterCase(TENANT_ALPHA, ACTOR_ALPHA, null, null, 1, TENANT_ALPHA, ACTOR_ALPHA)
            ),
            Arguments.of(
                "tenant-and-actor-mismatch",
                new HistoryFilterCase(TENANT_ALPHA, ACTOR_BETA, null, null, 0, null, null)
            ),
            Arguments.of(
                "range-wide",
                new HistoryFilterCase(null, null, "2000-01-01T00:00:00Z", "2100-01-01T00:00:00Z", 2, null, null)
            ),
            Arguments.of(
                "range-future-from",
                new HistoryFilterCase(null, null, "2100-01-01T00:00:00Z", null, 0, null, null)
            ),
            Arguments.of(
                "range-past-to",
                new HistoryFilterCase(null, null, null, "2000-01-01T00:00:00Z", 0, null, null)
            ),
            Arguments.of(
                "all-filters-hit",
                new HistoryFilterCase(
                    TENANT_BETA,
                    ACTOR_BETA.toUpperCase(),
                    "2000-01-01T00:00:00Z",
                    "2100-01-01T00:00:00Z",
                    1,
                    TENANT_BETA,
                    ACTOR_BETA
                )
            ),
            Arguments.of(
                "all-filters-miss",
                new HistoryFilterCase(
                    TENANT_BETA,
                    ACTOR_BETA,
                    "2100-01-01T00:00:00Z",
                    "2100-01-02T00:00:00Z",
                    0,
                    null,
                    null
                )
            )
        );
    }

    private static Stream<Arguments> invalidFilterCases() {
        return Stream.of(
            Arguments.of(
                "tenant-blank",
                new InvalidFilterCase("   ", null, null, null, "tenantCode query parameter must not be blank")
            ),
            Arguments.of(
                "changed-by-blank",
                new InvalidFilterCase(null, "   ", null, null, "changedBy query parameter must not be blank")
            ),
            Arguments.of(
                "changed-at-from-blank",
                new InvalidFilterCase(
                    null,
                    null,
                    "   ",
                    null,
                    "changedAtFrom query parameter must not be blank"
                )
            ),
            Arguments.of(
                "changed-at-to-blank",
                new InvalidFilterCase(
                    null,
                    null,
                    null,
                    "   ",
                    "changedAtTo query parameter must not be blank"
                )
            ),
            Arguments.of(
                "changed-at-from-invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    "not-a-timestamp",
                    null,
                    "changedAtFrom query parameter must be a valid ISO-8601 instant"
                )
            ),
            Arguments.of(
                "changed-at-to-invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    null,
                    "not-a-timestamp",
                    "changedAtTo query parameter must be a valid ISO-8601 instant"
                )
            ),
            Arguments.of(
                "changed-at-range-invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    "2026-03-02T00:00:00Z",
                    "2026-03-01T00:00:00Z",
                    "changedAtFrom must be before or equal to changedAtTo"
                )
            )
        );
    }

    private void seedAgreementStatusHistory(String agreementNumber) throws Exception {
        createAgreement(agreementNumber);
        registerActor(TENANT_ALPHA, ACTOR_ALPHA);
        registerActor(TENANT_BETA, ACTOR_BETA);

        transition(
            agreementNumber,
            AgreementStatus.ACTIVE,
            TENANT_ALPHA,
            "Initial activation",
            ACTOR_ALPHA
        )
            .andExpect(status().isOk());

        transition(
            agreementNumber,
            AgreementStatus.TERMINATED,
            TENANT_BETA,
            "Mutual termination",
            ACTOR_BETA
        )
            .andExpect(status().isOk());
    }

    private void createAgreement(String agreementNumber) throws Exception {
        String payload = """
            {
              "agreementNumber": "%s",
              "name": "History Filter Matrix Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """.formatted(agreementNumber);

        mockMvc.perform(post("/api/agreements").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }

    private ResultActions transition(
        String agreementNumber,
        AgreementStatus status,
        String tenantCode,
        String reason,
        String changedBy
    ) throws Exception {
        String payload = """
            {
              "status": "%s",
              "tenantCode": "%s",
              "reason": "%s",
              "changedBy": "%s"
            }
            """.formatted(status.name(), tenantCode, reason, changedBy);

        return mockMvc.perform(
            patch("/api/agreements/" + agreementNumber + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        );
    }

    private MockHttpServletRequestBuilder statusHistoryRequest(
        String agreementNumber,
        String tenantCode,
        String changedBy,
        String changedAtFrom,
        String changedAtTo
    ) {
        MockHttpServletRequestBuilder request = get("/api/agreements/" + agreementNumber + "/status-history")
            .param("page", "0")
            .param("size", "10");

        if (tenantCode != null) {
            request.param("tenantCode", tenantCode);
        }
        if (changedBy != null) {
            request.param("changedBy", changedBy);
        }
        if (changedAtFrom != null) {
            request.param("changedAtFrom", changedAtFrom);
        }
        if (changedAtTo != null) {
            request.param("changedAtTo", changedAtTo);
        }
        return request;
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "Agreements Status History Filter Matrix Tenant %s",
              "roleCode": "OPS",
              "roleName": "Operations",
              "email": "%s",
              "displayName": "Agreements Status History Filter Matrix Actor"
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

    private record HistoryFilterCase(
        String tenantCode,
        String changedBy,
        String changedAtFrom,
        String changedAtTo,
        int expectedTotalItems,
        String expectedTenantCode,
        String expectedChangedBy
    ) {}

    private record InvalidFilterCase(
        String tenantCode,
        String changedBy,
        String changedAtFrom,
        String changedAtTo,
        String expectedMessage
    ) {}
}
