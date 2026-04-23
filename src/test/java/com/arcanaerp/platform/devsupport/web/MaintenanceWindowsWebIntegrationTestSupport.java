package com.arcanaerp.platform.devsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class MaintenanceWindowsWebIntegrationTestSupport {

    private static final String MAINTENANCE_WINDOWS_PATH = "/api/dev-support/maintenance-windows";

    private MaintenanceWindowsWebIntegrationTestSupport() {}

    static ResultActions createMaintenanceWindow(
        MockMvc mockMvc,
        String tenantCode,
        String windowCode,
        String title,
        String description,
        String startsAt,
        String endsAt,
        boolean active
    ) throws Exception {
        return mockMvc.perform(post(MAINTENANCE_WINDOWS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "windowCode": "%s",
                  "title": "%s",
                  "description": "%s",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "active": %s
                }
                """.formatted(
                tenantCode,
                windowCode,
                title,
                description,
                startsAt,
                endsAt,
                active
            )));
    }

    static MockHttpServletRequestBuilder getMaintenanceWindowRequest(String tenantCode, String windowCode) {
        return get(MAINTENANCE_WINDOWS_PATH + "/" + windowCode).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listMaintenanceWindowsRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(MAINTENANCE_WINDOWS_PATH)
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

    static MockHttpServletRequestBuilder listMaintenanceWindowsRequest(String tenantCode) {
        return get(MAINTENANCE_WINDOWS_PATH).param("tenantCode", tenantCode);
    }
}
