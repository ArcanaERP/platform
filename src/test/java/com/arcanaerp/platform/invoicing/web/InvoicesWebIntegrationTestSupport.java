package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class InvoicesWebIntegrationTestSupport {

    static final String DEFAULT_STATUS_ACTOR = "invoices-system@arcanaerp.com";

    private InvoicesWebIntegrationTestSupport() {}

    static ResultActions registerProduct(MockMvc mockMvc, String sku) throws Exception {
        return ProductCatalogWebTestSupport.createProductWithDerivedCategory(
            mockMvc,
            sku,
            "Invoice Product",
            "Invoice Category",
            "10.00",
            "USD"
        );
    }

    static ResultActions createConfirmedSingleLineOrder(
        MockMvc mockMvc,
        InvoicesDeterministicClockTestSupport.AdjustableClock testClock,
        String orderNumber,
        String sku
    ) throws Exception {
        OrderManagementWebTestSupport.createSingleLineOrder(
            mockMvc,
            orderNumber,
            "buyer@acme.com",
            sku,
            "1",
            "10.00",
            "USD"
        ).andReturn();

        testClock.setInstant(InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60));
        return OrderManagementWebTestSupport.transitionOrderStatus(mockMvc, orderNumber, "CONFIRMED");
    }

    static ResultActions createInvoice(
        MockMvc mockMvc,
        String tenantCode,
        String invoiceNumber,
        String orderNumber,
        Instant dueAt
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "invoiceNumber": "%s",
              "orderNumber": "%s",
              "dueAt": "%s"
            }
            """.formatted(tenantCode, invoiceNumber, orderNumber, dueAt);

        return mockMvc.perform(post("/api/invoices")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static ResultActions transitionInvoiceStatus(MockMvc mockMvc, String invoiceNumber, String status) throws Exception {
        return transitionInvoiceStatus(
            mockMvc,
            invoiceNumber,
            status,
            "Invoice lifecycle transition",
            DEFAULT_STATUS_ACTOR
        );
    }

    static ResultActions transitionInvoiceStatus(
        MockMvc mockMvc,
        String invoiceNumber,
        String status,
        String reason,
        String changedBy
    ) throws Exception {
        String payload = """
            {
              "status": "%s",
              "reason": "%s",
              "changedBy": "%s"
            }
            """.formatted(status, reason, changedBy);

        return mockMvc.perform(patch("/api/invoices/" + invoiceNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static void registerInvoiceStatusActor(
        MockMvc mockMvc,
        String tenantCode,
        String email,
        String displayName
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/identity/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tenantCode": "%s",
                      "tenantName": "Invoice Tenant %s",
                      "roleCode": "BILLING",
                      "roleName": "Billing",
                      "email": "%s",
                      "displayName": "%s"
                    }
                    """.formatted(tenantCode, tenantCode, email, displayName)))
            .andReturn();
        int statusCode = result.getResponse().getStatus();
        if (statusCode == 201) {
            return;
        }
        if (statusCode == 400 && result.getResponse().getContentAsString().contains("User email already exists in tenant")) {
            return;
        }
        throw new AssertionError("Unexpected status while registering invoice status actor: " + statusCode);
    }

    static MockHttpServletRequestBuilder getInvoiceRequest(String invoiceNumber) {
        return get("/api/invoices/" + invoiceNumber);
    }

    static MockHttpServletRequestBuilder listInvoicesRequest(int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder request = get("/api/invoices")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must have an even number of elements");
        }
        for (int index = 0; index < optionalNameValuePairs.length; index += 2) {
            String name = optionalNameValuePairs[index];
            String value = optionalNameValuePairs[index + 1];
            if (value != null) {
                request.param(name, value);
            }
        }
        return request;
    }

    static MockHttpServletRequestBuilder dailyStatusActivitySummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivitySummaryRequest("daily", page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder dailyStatusActivityByCurrentStatusSummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityByCurrentStatusSummaryRequest("daily", page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder weeklyStatusActivitySummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivitySummaryRequest("weekly", page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder weeklyStatusActivityByCurrentStatusSummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityByCurrentStatusSummaryRequest("weekly", page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder monthlyStatusActivitySummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivitySummaryRequest("monthly", page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder monthlyStatusActivityByCurrentStatusSummaryRequest(
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityByCurrentStatusSummaryRequest("monthly", page, size, optionalNameValuePairs);
    }

    private static MockHttpServletRequestBuilder statusActivitySummaryRequest(
        String bucket,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/invoices/status-activity/" + bucket + "-summary")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must have an even number of elements");
        }
        for (int index = 0; index < optionalNameValuePairs.length; index += 2) {
            String name = optionalNameValuePairs[index];
            String value = optionalNameValuePairs[index + 1];
            if (value != null) {
                request.param(name, value);
            }
        }
        return request;
    }

    private static MockHttpServletRequestBuilder statusActivityByCurrentStatusSummaryRequest(
        String bucket,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/invoices/status-activity/" + bucket + "-summary/by-current-status")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must have an even number of elements");
        }
        for (int index = 0; index < optionalNameValuePairs.length; index += 2) {
            String name = optionalNameValuePairs[index];
            String value = optionalNameValuePairs[index + 1];
            if (value != null) {
                request.param(name, value);
            }
        }
        return request;
    }
}
