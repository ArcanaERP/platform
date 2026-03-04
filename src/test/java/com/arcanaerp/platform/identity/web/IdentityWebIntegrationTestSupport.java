package com.arcanaerp.platform.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class IdentityWebIntegrationTestSupport {

    private static final String USERS_PATH = "/api/identity/users";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private IdentityWebIntegrationTestSupport() {}

    static ResultActions createUser(
        MockMvc mockMvc,
        String tenantCode,
        String tenantName,
        String roleCode,
        String roleName,
        String email,
        String displayName
    ) throws Exception {
        return mockMvc.perform(post(USERS_PATH).contentType(MediaType.APPLICATION_JSON).content(
            createUserPayload(tenantCode, tenantName, roleCode, roleName, email, displayName)
        ));
    }

    static String createUserPayload(
        String tenantCode,
        String tenantName,
        String roleCode,
        String roleName,
        String email,
        String displayName
    ) {
        return """
            {
              "tenantCode": "%s",
              "tenantName": "%s",
              "roleCode": "%s",
              "roleName": "%s",
              "email": "%s",
              "displayName": "%s"
            }
            """.formatted(tenantCode, tenantName, roleCode, roleName, email, displayName);
    }

    static MockHttpServletRequestBuilder listUsersRequest(int page, int size) {
        return get(USERS_PATH)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder listUsersRequest() {
        return get(USERS_PATH);
    }

    static MockHttpServletRequestBuilder listUsersRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = listUsersRequest(page, size);
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

    static MockHttpServletRequestBuilder listUsersRequestDefault(String... optionalNameValuePairs) {
        return listUsersRequest(DEFAULT_PAGE, DEFAULT_SIZE, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder getUserRequest(String userId) {
        return get(USERS_PATH + "/" + userId);
    }
}
