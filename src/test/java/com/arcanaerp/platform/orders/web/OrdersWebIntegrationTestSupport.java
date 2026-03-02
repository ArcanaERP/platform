package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import java.time.Instant;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class OrdersWebIntegrationTestSupport {

    private static final String STATUS_HISTORY_PATH_FORMAT = "/api/orders/%s/status-history";

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
}
