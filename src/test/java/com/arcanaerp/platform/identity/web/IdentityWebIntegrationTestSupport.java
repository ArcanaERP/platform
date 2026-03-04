package com.arcanaerp.platform.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class IdentityWebIntegrationTestSupport {

    private static final String USERS_PATH = "/api/identity/users";

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
}
