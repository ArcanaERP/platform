package com.arcanaerp.platform.workeffort.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class WorkEffortsWebIntegrationTestSupport {

    private static final String WORK_EFFORTS_PATH = "/api/work-efforts";

    private WorkEffortsWebIntegrationTestSupport() {}

    static ResultActions createWorkEffort(
        MockMvc mockMvc,
        String tenantCode,
        String effortNumber,
        String name,
        String description,
        String status,
        String assignedTo,
        String dueAt
    ) throws Exception {
        String formattedDueAt = dueAt == null ? "null" : "\"" + dueAt + "\"";
        return mockMvc.perform(post(WORK_EFFORTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "effortNumber": "%s",
                  "name": "%s",
                  "description": "%s",
                  "status": "%s",
                  "assignedTo": "%s",
                  "dueAt": %s
                }
                """.formatted(
                tenantCode,
                effortNumber,
                name,
                description,
                status,
                assignedTo,
                formattedDueAt
            )));
    }

    static MockHttpServletRequestBuilder getWorkEffortRequest(String tenantCode, String effortNumber) {
        return get(WORK_EFFORTS_PATH + "/" + effortNumber).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listWorkEffortsRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH)
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

    static MockHttpServletRequestBuilder listWorkEffortsRequest(String tenantCode) {
        return get(WORK_EFFORTS_PATH).param("tenantCode", tenantCode);
    }
}
