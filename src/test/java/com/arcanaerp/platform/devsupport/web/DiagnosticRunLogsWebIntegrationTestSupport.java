package com.arcanaerp.platform.devsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class DiagnosticRunLogsWebIntegrationTestSupport {

    private static final String DIAGNOSTIC_RUN_LOGS_PATH = "/api/dev-support/diagnostic-run-logs";

    private DiagnosticRunLogsWebIntegrationTestSupport() {}

    static ResultActions createDiagnosticRunLog(
        MockMvc mockMvc,
        String tenantCode,
        String runNumber,
        String diagnosticCode,
        String title,
        String summary,
        DiagnosticRunStatus status,
        String startedAt,
        String finishedAt
    ) throws Exception {
        String finishedAtProperty = finishedAt == null
            ? "\"finishedAt\": null"
            : "\"finishedAt\": \"%s\"".formatted(finishedAt);

        return mockMvc.perform(post(DIAGNOSTIC_RUN_LOGS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "runNumber": "%s",
                  "diagnosticCode": "%s",
                  "title": "%s",
                  "summary": "%s",
                  "status": "%s",
                  "startedAt": "%s",
                  %s
                }
                """.formatted(
                tenantCode,
                runNumber,
                diagnosticCode,
                title,
                summary,
                status.name(),
                startedAt,
                finishedAtProperty
            )));
    }

    static MockHttpServletRequestBuilder getDiagnosticRunLogRequest(String tenantCode, String runNumber) {
        return get(DIAGNOSTIC_RUN_LOGS_PATH + "/" + runNumber).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listDiagnosticRunLogsRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(DIAGNOSTIC_RUN_LOGS_PATH)
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

    static MockHttpServletRequestBuilder listDiagnosticRunLogsRequest(String tenantCode) {
        return get(DIAGNOSTIC_RUN_LOGS_PATH).param("tenantCode", tenantCode);
    }
}
