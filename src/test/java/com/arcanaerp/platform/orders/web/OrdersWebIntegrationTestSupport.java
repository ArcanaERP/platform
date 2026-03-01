package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class OrdersWebIntegrationTestSupport {

    private static final String STATUS_HISTORY_PATH_FORMAT = "/api/orders/%s/status-history";

    private OrdersWebIntegrationTestSupport() {}

    static ResultActions registerProduct(MockMvc mockMvc, String sku, String productNamePrefix, String categoryName) throws Exception {
        String normalizedSku = normalizeSku(sku);
        String categoryCode = categoryCodeFromSku(normalizedSku);
        String payload = """
            {
              "sku": "%s",
              "name": "%s %s",
              "categoryCode": "%s",
              "categoryName": "%s",
              "amount": 1.00,
              "currencyCode": "USD"
            }
            """.formatted(sku, productNamePrefix, normalizedSku, categoryCode, categoryName);

        return mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static ResultActions createSingleLineOrder(
        MockMvc mockMvc,
        String orderNumber,
        String sku,
        String customerEmail
    ) throws Exception {
        String payload = """
            {
              "orderNumber": "%s",
              "customerEmail": "%s",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "%s", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """.formatted(orderNumber, customerEmail, sku);

        return mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static ResultActions transitionToConfirmed(
        MockMvc mockMvc,
        OrdersDeterministicClockTestSupport.AdjustableClock testClock,
        String orderNumber,
        Instant confirmedAt
    ) throws Exception {
        testClock.setInstant(confirmedAt);
        return mockMvc.perform(patch("/api/orders/" + orderNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"CONFIRMED\"}"));
    }

    static ResultActions seedConfirmedStatusHistory(
        MockMvc mockMvc,
        OrdersDeterministicClockTestSupport.AdjustableClock testClock,
        String orderNumber,
        String sku,
        String productNamePrefix,
        String categoryName,
        String customerEmail,
        Instant confirmedAt
    ) throws Exception {
        registerProduct(mockMvc, sku, productNamePrefix, categoryName)
            .andExpect(status().isCreated());
        createSingleLineOrder(mockMvc, orderNumber, sku, customerEmail)
            .andExpect(status().isCreated());
        return transitionToConfirmed(mockMvc, testClock, orderNumber, confirmedAt);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(String orderNumber) {
        return get(STATUS_HISTORY_PATH_FORMAT.formatted(orderNumber));
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String orderNumber,
        Integer page,
        Integer size,
        String previousStatus,
        String currentStatus,
        String changedAtFrom,
        String changedAtTo
    ) {
        MockHttpServletRequestBuilder request = statusHistoryRequest(orderNumber);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
        if (previousStatus != null) {
            request.param("previousStatus", previousStatus);
        }
        if (currentStatus != null) {
            request.param("currentStatus", currentStatus);
        }
        if (changedAtFrom != null) {
            request.param("changedAtFrom", changedAtFrom);
        }
        if (changedAtTo != null) {
            request.param("changedAtTo", changedAtTo);
        }
        return request;
    }

    private static String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private static String categoryCodeFromSku(String normalizedSku) {
        String sanitized = normalizedSku.replaceAll("[^A-Z0-9]", "");
        String fullCode = "CAT" + sanitized;
        return fullCode.substring(0, Math.min(32, fullCode.length()));
    }
}
