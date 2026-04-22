package com.arcanaerp.platform.devsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class DevSupportWebIntegrationTestSupport {

    private static final String SYSTEM_NOTICES_PATH = "/api/dev-support/system-notices";

    private DevSupportWebIntegrationTestSupport() {}

    static ResultActions createSystemNotice(
        MockMvc mockMvc,
        String tenantCode,
        String noticeCode,
        String title,
        String message,
        NoticeSeverity severity,
        boolean active
    ) throws Exception {
        return mockMvc.perform(post(SYSTEM_NOTICES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "noticeCode": "%s",
                  "title": "%s",
                  "message": "%s",
                  "severity": "%s",
                  "active": %s
                }
                """.formatted(
                tenantCode,
                noticeCode,
                title,
                message,
                severity.name(),
                active
            )));
    }

    static MockHttpServletRequestBuilder getSystemNoticeRequest(String tenantCode, String noticeCode) {
        return get(SYSTEM_NOTICES_PATH + "/" + noticeCode).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listSystemNoticesRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(SYSTEM_NOTICES_PATH)
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

    static MockHttpServletRequestBuilder listSystemNoticesRequest(String tenantCode) {
        return get(SYSTEM_NOTICES_PATH).param("tenantCode", tenantCode);
    }
}
