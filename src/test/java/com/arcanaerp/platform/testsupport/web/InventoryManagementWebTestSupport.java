package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class InventoryManagementWebTestSupport {

    private InventoryManagementWebTestSupport() {}

    public static ResultActions adjustInventory(MockMvc mockMvc, String sku, String payload) throws Exception {
        return mockMvc.perform(post("/api/inventory/{sku}/adjustments", sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static ResultActions adjustInventory(MockMvc mockMvc, String sku, String locationCode, String payload) throws Exception {
        return mockMvc.perform(post("/api/inventory/{sku}/adjustments", sku)
            .param("locationCode", locationCode)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static ResultActions transferInventory(MockMvc mockMvc, String sku, String payload) throws Exception {
        return mockMvc.perform(post("/api/inventory/{sku}/transfers", sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static String adjustmentPayload(String quantityDelta, String reason, String adjustedBy) {
        return """
            {
              "quantityDelta": %s,
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(quantityDelta, reason, adjustedBy);
    }

    public static String transferPayload(
        String sourceLocationCode,
        String destinationLocationCode,
        String quantity,
        String reason,
        String adjustedBy
    ) {
        return """
            {
              "sourceLocationCode": "%s",
              "destinationLocationCode": "%s",
              "quantity": %s,
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(sourceLocationCode, destinationLocationCode, quantity, reason, adjustedBy);
    }

    public static String transferPayload(
        String sourceLocationCode,
        String destinationLocationCode,
        String quantity,
        String reason,
        String adjustedBy,
        String referenceType,
        String referenceId
    ) {
        return """
            {
              "sourceLocationCode": "%s",
              "destinationLocationCode": "%s",
              "quantity": %s,
              "reason": "%s",
              "adjustedBy": "%s",
              "referenceType": "%s",
              "referenceId": "%s"
            }
            """.formatted(sourceLocationCode, destinationLocationCode, quantity, reason, adjustedBy, referenceType, referenceId);
    }
}
