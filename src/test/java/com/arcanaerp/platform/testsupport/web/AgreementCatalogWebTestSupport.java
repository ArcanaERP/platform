package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public final class AgreementCatalogWebTestSupport {

    private AgreementCatalogWebTestSupport() {}

    public static ResultActions createAgreement(MockMvc mockMvc, String agreementNumber, String name) throws Exception {
        return mockMvc.perform(
            post("/api/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAgreementPayload(agreementNumber, name))
        );
    }

    public static String createAgreementPayload(String agreementNumber, String name) {
        return """
            {
              "agreementNumber": "%s",
              "name": "%s",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """.formatted(agreementNumber, name);
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
}
