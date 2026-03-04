package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.OrderStatusHistoryWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import java.time.Instant;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class OrdersWebIntegrationTestSupport {

    private OrdersWebIntegrationTestSupport() {}

    static ResultActions registerProduct(MockMvc mockMvc, String sku, String productNamePrefix, String categoryName) throws Exception {
        return ProductCatalogWebTestSupport.createProductWithDerivedCategory(
            mockMvc,
            sku,
            productNamePrefix,
            categoryName,
            "1.00",
            "USD"
        );
    }

    static ResultActions createSingleLineOrder(
        MockMvc mockMvc,
        String orderNumber,
        String sku,
        String customerEmail
    ) throws Exception {
        return OrderManagementWebTestSupport.createSingleLineOrder(
            mockMvc,
            orderNumber,
            customerEmail,
            sku,
            "1",
            "10.00",
            "USD"
        );
    }

    static ResultActions transitionToConfirmed(
        MockMvc mockMvc,
        OrdersDeterministicClockTestSupport.AdjustableClock testClock,
        String orderNumber,
        Instant confirmedAt
    ) throws Exception {
        testClock.setInstant(confirmedAt);
        return OrderManagementWebTestSupport.transitionOrderStatus(mockMvc, orderNumber, "CONFIRMED");
    }

    static ResultActions registerActor(MockMvc mockMvc, String tenantCode, String email, String displayName) throws Exception {
        return ActorActivationWebTestSupport.registerActor(
            mockMvc,
            tenantCode,
            email,
            "Order Tenant",
            displayName
        );
    }

    static ResultActions setProductActive(
        MockMvc mockMvc,
        String sku,
        boolean active,
        String tenantCode,
        String changedBy,
        String reason
    ) throws Exception {
        return ActorActivationWebTestSupport.setProductActive(mockMvc, sku, active, tenantCode, changedBy, reason);
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
        return OrderStatusHistoryWebTestSupport.statusHistoryRequest(orderNumber);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String orderNumber,
        Integer page,
        Integer size
    ) {
        return OrderStatusHistoryWebTestSupport.statusHistoryRequest(orderNumber, page, size);
    }

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(String orderNumber) {
        return OrderStatusHistoryWebTestSupport.statusHistoryRequestDefault(orderNumber);
    }

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(
        String orderNumber,
        String... optionalNameValuePairs
    ) {
        return OrderStatusHistoryWebTestSupport.statusHistoryRequestDefault(orderNumber, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String orderNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return OrderStatusHistoryWebTestSupport.statusHistoryRequest(
            orderNumber,
            page,
            size,
            optionalNameValuePairs
        );
    }
}
