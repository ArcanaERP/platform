package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class OrderManagementWebTestSupport {

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
        String payload = """
            {
              "orderNumber": "%s",
              "customerEmail": "%s",
              "currencyCode": "%s",
              "lines": [
                { "productSku": "%s", "quantity": %s, "unitPrice": %s }
              ]
            }
            """.formatted(orderNumber, customerEmail, currencyCode, sku, quantity, unitPrice);

        return mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }
}
