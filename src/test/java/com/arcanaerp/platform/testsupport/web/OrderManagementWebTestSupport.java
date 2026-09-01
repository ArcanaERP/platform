package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class OrderManagementWebTestSupport {

    public static final String DEFAULT_ORDER_TENANT = "tenant-orders";

    private OrderManagementWebTestSupport() {}

    public static ResultActions createSingleLineOrder(
        MockMvc mockMvc,
        String orderNumber,
        String customerEmail,
        String sku,
        String quantity,
        String unitPrice,
        String currencyCode
    ) throws Exception {
        return createSingleLineOrder(
            mockMvc,
            DEFAULT_ORDER_TENANT,
            orderNumber,
            customerEmail,
            sku,
            quantity,
            unitPrice,
            currencyCode
        );
    }

    public static ResultActions createSingleLineOrder(
        MockMvc mockMvc,
        String tenantCode,
        String orderNumber,
        String customerEmail,
        String sku,
        String quantity,
        String unitPrice,
        String currencyCode
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "orderNumber": "%s",
              "customerEmail": "%s",
              "currencyCode": "%s",
              "lines": [
                { "productSku": "%s", "quantity": %s, "unitPrice": %s }
              ]
            }
            """.formatted(tenantCode, orderNumber, customerEmail, currencyCode, sku, quantity, unitPrice);

        return mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static ResultActions createOrder(
        MockMvc mockMvc,
        String orderNumber,
        String customerEmail,
        String currencyCode,
        OrderLineRequest... lines
    ) throws Exception {
        return createOrder(
            mockMvc,
            DEFAULT_ORDER_TENANT,
            orderNumber,
            customerEmail,
            currencyCode,
            lines
        );
    }

    public static ResultActions createOrder(
        MockMvc mockMvc,
        String tenantCode,
        String orderNumber,
        String customerEmail,
        String currencyCode,
        OrderLineRequest... lines
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "orderNumber": "%s",
              "customerEmail": "%s",
              "currencyCode": "%s",
              "lines": [%s]
            }
            """.formatted(tenantCode, orderNumber, customerEmail, currencyCode, joinLinePayloads(lines));

        return mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static OrderLineRequest line(String productSku, String quantity, String unitPrice) {
        return new OrderLineRequest(productSku, quantity, unitPrice);
    }

    public static ResultActions transitionOrderStatus(MockMvc mockMvc, String orderNumber, String status) throws Exception {
        return transitionOrderStatus(
            mockMvc,
            orderNumber,
            status,
            "Order lifecycle transition",
            "orders-system@arcanaerp.com"
        );
    }

    public static ResultActions transitionOrderStatus(
        MockMvc mockMvc,
        String orderNumber,
        String status,
        String reason,
        String changedBy
    ) throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            DEFAULT_ORDER_TENANT,
            changedBy,
            "Order Tenant",
            "Order Status Actor"
        );

        String payload = """
            {
              "status": "%s",
              "reason": "%s",
              "changedBy": "%s"
            }
            """.formatted(status, reason, changedBy);

        return mockMvc.perform(patch("/api/orders/" + orderNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static MockHttpServletRequestBuilder getOrderRequest(String orderNumber) {
        return get("/api/orders/" + orderNumber);
    }

    public static MockHttpServletRequestBuilder listOrdersRequest(int page, int size) {
        return get("/api/orders")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    public static MockHttpServletRequestBuilder listOrdersRequest(String tenantCode, int page, int size) {
        return listOrdersRequest(page, size).param("tenantCode", tenantCode);
    }

    public record OrderLineRequest(String productSku, String quantity, String unitPrice) {}

    private static String joinLinePayloads(OrderLineRequest[] lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append("""
                { "productSku": "%s", "quantity": %s, "unitPrice": %s }
                """.formatted(lines[i].productSku(), lines[i].quantity(), lines[i].unitPrice()));
        }
        return builder.toString();
    }
}
