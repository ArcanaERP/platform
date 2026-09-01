package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public final class AgreementCatalogWebTestSupport {

    public static final String DEFAULT_AGREEMENT_TENANT = "tenant-agreements";

    private AgreementCatalogWebTestSupport() {}

    public static ResultActions createAgreement(MockMvc mockMvc, String agreementNumber, String name) throws Exception {
        return createAgreement(mockMvc, DEFAULT_AGREEMENT_TENANT, agreementNumber, name);
    }

    public static ResultActions createAgreement(
        MockMvc mockMvc,
        String tenantCode,
        String agreementNumber,
        String name
    ) throws Exception {
        return mockMvc.perform(
            post("/api/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAgreementPayload(tenantCode, agreementNumber, name))
        );
    }

    public static String createAgreementPayload(String agreementNumber, String name) {
        return createAgreementPayload(DEFAULT_AGREEMENT_TENANT, agreementNumber, name);
    }

    public static String createAgreementPayload(String tenantCode, String agreementNumber, String name) {
        return """
            {
              "tenantCode": "%s",
              "agreementNumber": "%s",
              "name": "%s",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """.formatted(tenantCode, agreementNumber, name);
    }

    public static MockHttpServletRequestBuilder listAgreementsRequest(int page, int size) {
        return get("/api/agreements")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    public static MockHttpServletRequestBuilder listAgreementsRequest(int page, int size, String status) {
        MockHttpServletRequestBuilder request = listAgreementsRequest(page, size);
        if (status != null) {
            request.param("status", status);
        }
        return request;
    }

    public static MockHttpServletRequestBuilder listAgreementsRequest(
        int page,
        int size,
        String tenantCode,
        String status
    ) {
        MockHttpServletRequestBuilder request = listAgreementsRequest(page, size, status);
        if (tenantCode != null) {
            request.param("tenantCode", tenantCode);
        }
        return request;
    }

    public static MockHttpServletRequestBuilder getAgreementRequest(String agreementNumber) {
        return get("/api/agreements/" + agreementNumber);
    }
}
