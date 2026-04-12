package com.arcanaerp.platform.core.uom.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class CoreWebIntegrationTestSupport {

    private static final String UOM_PATH = "/api/core/units-of-measurement";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private CoreWebIntegrationTestSupport() {}

    static ResultActions createUnitOfMeasurement(
        MockMvc mockMvc,
        String code,
        String description,
        String domain,
        String comments
    ) throws Exception {
        return mockMvc.perform(
            post(UOM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUnitOfMeasurementPayload(code, description, domain, comments))
        );
    }

    static String createUnitOfMeasurementPayload(String code, String description, String domain, String comments) {
        return """
            {
              "code": "%s",
              "description": "%s",
              "domain": %s,
              "comments": %s
            }
            """.formatted(code, description, jsonStringOrNull(domain), jsonStringOrNull(comments));
    }

    static MockHttpServletRequestBuilder listUnitsOfMeasurementRequest(int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(UOM_PATH)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs == null || optionalNameValuePairs.length == 0) {
            return builder;
        }
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must contain name/value pairs");
        }
        for (int i = 0; i < optionalNameValuePairs.length; i += 2) {
            builder.param(optionalNameValuePairs[i], optionalNameValuePairs[i + 1]);
        }
        return builder;
    }

    static MockHttpServletRequestBuilder listUnitsOfMeasurementRequest() {
        return get(UOM_PATH);
    }

    static MockHttpServletRequestBuilder listUnitsOfMeasurementRequestDefault(String... optionalNameValuePairs) {
        return listUnitsOfMeasurementRequest(DEFAULT_PAGE, DEFAULT_SIZE, optionalNameValuePairs);
    }

    private static String jsonStringOrNull(String value) {
        return value == null ? "null" : "\"%s\"".formatted(value);
    }
}
