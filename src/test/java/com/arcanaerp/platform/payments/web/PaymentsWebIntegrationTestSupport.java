package com.arcanaerp.platform.payments.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class PaymentsWebIntegrationTestSupport {

    private PaymentsWebIntegrationTestSupport() {}

    static ResultActions createPayment(
        MockMvc mockMvc,
        String tenantCode,
        String paymentReference,
        String invoiceNumber,
        String amount,
        String currencyCode,
        Instant paidAt
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "paymentReference": "%s",
              "invoiceNumber": "%s",
              "amount": %s,
              "currencyCode": "%s",
              "paidAt": "%s"
            }
            """.formatted(tenantCode, paymentReference, invoiceNumber, amount, currencyCode, paidAt);

        return mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static MockHttpServletRequestBuilder invoiceBalanceRequest(String invoiceNumber) {
        return get("/api/payments/invoices/" + invoiceNumber + "/balance");
    }

    static MockHttpServletRequestBuilder listPaymentsRequest(int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder request = get("/api/payments")
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

    static MockHttpServletRequestBuilder tenantSummaryRequest(
        String tenantCode,
        String currencyCode,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/summary")
            .param("currencyCode", currencyCode);
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

    static void seedIssuedInvoice(
        MockMvc mockMvc,
        PaymentsDeterministicClockTestSupport.AdjustableClock testClock,
        String sku,
        String orderNumber,
        String invoiceNumber
    ) throws Exception {
        ProductCatalogWebTestSupport.createProductWithDerivedCategory(
            mockMvc,
            sku,
            "Payment Product",
            "Payment Category",
            "10.00",
            "USD"
        ).andReturn();
        OrderManagementWebTestSupport.createSingleLineOrder(
            mockMvc,
            orderNumber,
            "buyer@acme.com",
            sku,
            "1",
            "10.00",
            "USD"
        ).andReturn();
        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60));
        OrderManagementWebTestSupport.transitionOrderStatus(mockMvc, orderNumber, "CONFIRMED").andReturn();
        testClock.resetToBaseInstant();
        createInvoice(mockMvc, "tenant-pay", invoiceNumber, orderNumber, PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(86400))
            .andReturn();
        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120));
        transitionInvoiceStatus(mockMvc, invoiceNumber, "ISSUED").andReturn();
        testClock.resetToBaseInstant();
    }

    static ResultActions transitionInvoiceStatus(MockMvc mockMvc, String invoiceNumber, String status) throws Exception {
        String payload = """
            {
              "status": "%s"
            }
            """.formatted(status);

        return mockMvc.perform(patch("/api/invoices/" + invoiceNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    private static ResultActions createInvoice(
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
}
