package com.arcanaerp.platform.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class IdentityWebIntegrationTestSupport {

    private static final String USERS_PATH = "/api/identity/users";
    private static final String ROLES_PATH = "/api/identity/roles";
    private static final String TENANTS_PATH = "/api/identity/tenants";
    private static final String ORG_UNITS_PATH = "/api/identity/org-units";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private IdentityWebIntegrationTestSupport() {}

    static ResultActions createTenant(
        MockMvc mockMvc,
        String code,
        String name
    ) throws Exception {
        return mockMvc.perform(post(TENANTS_PATH).contentType(MediaType.APPLICATION_JSON).content(
            createTenantPayload(code, name)
        ));
    }

    static String createTenantPayload(String code, String name) {
        return """
            {
              "code": "%s",
              "name": "%s"
            }
            """.formatted(code, name);
    }

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

    static MockHttpServletRequestBuilder listRolesRequest(String tenantCode, int page, int size) {
        return get(ROLES_PATH)
            .param("tenantCode", tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder listRolesRequest(String tenantCode) {
        return get(ROLES_PATH)
            .param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listTenantsRequest(int page, int size) {
        return get(TENANTS_PATH)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder listTenantsRequest() {
        return get(TENANTS_PATH);
    }

    static MockHttpServletRequestBuilder createTenantRequest(String payload) {
        return post(TENANTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload);
    }

    static MockHttpServletRequestBuilder getTenantRequest(String code) {
        return get(TENANTS_PATH + "/" + code);
    }

    static ResultActions createRole(
        MockMvc mockMvc,
        String tenantCode,
        String tenantName,
        String code,
        String name
    ) throws Exception {
        return mockMvc.perform(post(ROLES_PATH).contentType(MediaType.APPLICATION_JSON).content(
            createRolePayload(tenantCode, tenantName, code, name)
        ));
    }

    static String createRolePayload(String tenantCode, String tenantName, String code, String name) {
        return """
            {
              "tenantCode": "%s",
              "tenantName": "%s",
              "code": "%s",
              "name": "%s"
            }
            """.formatted(tenantCode, tenantName, code, name);
    }

    static MockHttpServletRequestBuilder getUserRequest(String userId) {
        return get(USERS_PATH + "/" + userId);
    }

    static ResultActions createOrgUnit(
        MockMvc mockMvc,
        String tenantCode,
        String tenantName,
        String code,
        String name
    ) throws Exception {
        return mockMvc.perform(post(ORG_UNITS_PATH).contentType(MediaType.APPLICATION_JSON).content(
            createOrgUnitPayload(tenantCode, tenantName, code, name)
        ));
    }

    static String createOrgUnitPayload(
        String tenantCode,
        String tenantName,
        String code,
        String name
    ) {
        return """
            {
              "tenantCode": "%s",
              "tenantName": "%s",
              "code": "%s",
              "name": "%s"
            }
            """.formatted(tenantCode, tenantName, code, name);
    }

    static MockHttpServletRequestBuilder listOrgUnitsRequest(String tenantCode) {
        return get(ORG_UNITS_PATH)
            .param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listOrgUnitsRequest(String tenantCode, int page, int size) {
        return listOrgUnitsRequest(tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder listOrgUnitsRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = listOrgUnitsRequest(tenantCode, page, size);
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

    static MockHttpServletRequestBuilder getOrgUnitRequest(String tenantCode, String code) {
        return get(ORG_UNITS_PATH + "/" + code)
            .param("tenantCode", tenantCode);
    }

    static ResultActions updateOrgUnit(
        MockMvc mockMvc,
        String tenantCode,
        String code,
        String name,
        Boolean active
    ) throws Exception {
        return mockMvc.perform(
            patch(ORG_UNITS_PATH + "/" + code)
                .param("tenantCode", tenantCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateOrgUnitPayload(name, active))
        );
    }

    static String updateOrgUnitPayload(String name, Boolean active) {
        String activeValue = active == null ? "null" : active.toString();
        return """
            {
              "name": "%s",
              "active": %s
            }
            """.formatted(name, activeValue);
    }
}
