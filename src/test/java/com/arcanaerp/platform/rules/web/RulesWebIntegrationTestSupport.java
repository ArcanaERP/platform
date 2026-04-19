package com.arcanaerp.platform.rules.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class RulesWebIntegrationTestSupport {

    private static final String RULES_PATH = "/api/rules";

    private RulesWebIntegrationTestSupport() {}

    static ResultActions createRuleDefinition(
        MockMvc mockMvc,
        String tenantCode,
        String code,
        String name,
        String appliesTo,
        String expression,
        String description,
        boolean active
    ) throws Exception {
        return mockMvc.perform(post(RULES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "code": "%s",
                  "name": "%s",
                  "appliesTo": "%s",
                  "expression": "%s",
                  "description": %s,
                  "active": %s
                }
                """.formatted(
                tenantCode,
                code,
                name,
                appliesTo,
                expression,
                jsonStringOrNull(description),
                active
            )));
    }

    static MockHttpServletRequestBuilder getRuleDefinitionRequest(String tenantCode, String code) {
        return get(RULES_PATH + "/" + code).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listRuleDefinitionsRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(RULES_PATH)
            .param("tenantCode", tenantCode)
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

    static MockHttpServletRequestBuilder listRuleDefinitionsRequest(String tenantCode) {
        return get(RULES_PATH).param("tenantCode", tenantCode);
    }

    private static String jsonStringOrNull(String value) {
        return value == null ? "null" : "\"%s\"".formatted(value);
    }
}
