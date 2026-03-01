package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public final class ActorActivationWebTestSupport {

    private ActorActivationWebTestSupport() {}

    public static ResultActions registerActor(
        MockMvc mockMvc,
        String tenantCode,
        String email,
        String tenantNamePrefix,
        String displayName
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "%s %s",
              "roleCode": "OPS",
              "roleName": "Operations",
              "email": "%s",
              "displayName": "%s"
            }
            """.formatted(tenantCode, tenantNamePrefix, tenantCode, email, displayName);

        return mockMvc.perform(post("/api/identity/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static void registerActorAllowingDuplicateEmail(
        MockMvc mockMvc,
        String tenantCode,
        String email,
        String tenantNamePrefix,
        String displayName
    ) throws Exception {
        MvcResult result = registerActor(mockMvc, tenantCode, email, tenantNamePrefix, displayName)
            .andReturn();
        int statusCode = result.getResponse().getStatus();
        if (statusCode == 201) {
            return;
        }
        if (statusCode == 400 && result.getResponse().getContentAsString().contains("User email already exists in tenant")) {
            return;
        }
        throw new AssertionError("Unexpected status while registering actor: " + statusCode);
    }

    public static ResultActions setProductActive(
        MockMvc mockMvc,
        String sku,
        boolean active,
        String tenantCode,
        String changedBy,
        String reason
    ) throws Exception {
        String payload = """
            {
              "active": %s,
              "reason": "%s",
              "tenantCode": "%s",
              "changedBy": "%s"
            }
            """.formatted(active, reason, tenantCode, changedBy);

        return mockMvc.perform(patch("/api/products/{sku}/active", sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }
}
