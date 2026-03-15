package com.arcanaerp.platform.payments.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;

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

    static MockHttpServletRequestBuilder tenantReceivablesRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/receivables")
            .param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
        return request;
    }

    static MockHttpServletRequestBuilder tenantReceivablesSummaryRequest(
        String tenantCode,
        String currencyCode
    ) {
        return get("/api/payments/tenants/" + tenantCode + "/receivables/summary")
            .param("currencyCode", currencyCode);
    }

    static MockHttpServletRequestBuilder tenantReceivablesAgingRequest(
        String tenantCode,
        String currencyCode
    ) {
        return get("/api/payments/tenants/" + tenantCode + "/receivables/aging")
            .param("currencyCode", currencyCode);
    }

    static MockHttpServletRequestBuilder tenantReceivablesByAgingBucketRequest(
        String tenantCode,
        String agingBucket,
        String currencyCode,
        Integer page,
        Integer size
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/aging/" + agingBucket
        ).param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
        return request;
    }

    static MockHttpServletRequestBuilder over90CollectionsQueueRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/over-90"
        ).param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static ResultActions assignOver90CollectionsInvoice(
        MockMvc mockMvc,
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String assignedBy
    ) throws Exception {
        String payload = """
            {
              "assignedTo": "%s",
              "assignedBy": "%s"
            }
            """.formatted(assignedTo, assignedBy);

        return mockMvc.perform(post(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/over-90/" + invoiceNumber + "/assignment"
        ).contentType(MediaType.APPLICATION_JSON).content(payload));
    }

    static MockHttpServletRequestBuilder collectionsAssignmentHistoryRequest(
        String tenantCode,
        String invoiceNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/over-90/" + invoiceNumber + "/assignment-history"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static ResultActions addCollectionsNote(
        MockMvc mockMvc,
        String tenantCode,
        String invoiceNumber,
        String note,
        String notedBy,
        String category,
        String outcome
    ) throws Exception {
        String payload = """
            {
              "note": "%s",
              "notedBy": "%s",
              "category": "%s",
              "outcome": "%s"
            }
            """.formatted(note, notedBy, category, outcome);

        return mockMvc.perform(post(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/over-90/" + invoiceNumber + "/notes"
        ).contentType(MediaType.APPLICATION_JSON).content(payload));
    }

    static MockHttpServletRequestBuilder collectionsNotesRequest(
        String tenantCode,
        String invoiceNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/over-90/" + invoiceNumber + "/notes"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder tenantCollectionsNotesRequest(
        String tenantCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/notes"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder tenantCollectionsAssignmentHistoryRequest(
        String tenantCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/assignment-history"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder tenantCollectionsAssignmentSummaryRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/summary"
        ).param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
        return request;
    }

    static MockHttpServletRequestBuilder dailyTenantCollectionsAssignmentSummaryRequest(
        String tenantCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/daily-summary"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder weeklyTenantCollectionsAssignmentSummaryRequest(
        String tenantCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/weekly-summary"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder monthlyTenantCollectionsAssignmentSummaryRequest(
        String tenantCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get(
            "/api/payments/tenants/" + tenantCode + "/receivables/collections/monthly-summary"
        );
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static ResultActions createIdentityUser(
        MockMvc mockMvc,
        String tenantCode,
        String tenantName,
        String roleCode,
        String roleName,
        String email,
        String displayName
    ) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "%s",
              "roleCode": "%s",
              "roleName": "%s",
              "email": "%s",
              "displayName": "%s"
            }
            """.formatted(tenantCode, tenantName, roleCode, roleName, email, displayName);

        return mockMvc.perform(post("/api/identity/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
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

    static MockHttpServletRequestBuilder tenantInvoiceSummaryRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/invoices")
            .param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder dailyTenantSummaryRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/daily-summary")
            .param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder monthlyTenantSummaryRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/monthly-summary")
            .param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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

    static MockHttpServletRequestBuilder weeklyTenantSummaryRequest(
        String tenantCode,
        String currencyCode,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = get("/api/payments/tenants/" + tenantCode + "/weekly-summary")
            .param("currencyCode", currencyCode);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
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
        seedIssuedInvoice(mockMvc, testClock, "tenant-pay", sku, orderNumber, invoiceNumber);
    }

    static void seedIssuedInvoice(
        MockMvc mockMvc,
        PaymentsDeterministicClockTestSupport.AdjustableClock testClock,
        String tenantCode,
        String sku,
        String orderNumber,
        String invoiceNumber
    ) throws Exception {
        seedIssuedInvoice(
            mockMvc,
            testClock,
            tenantCode,
            sku,
            orderNumber,
            invoiceNumber,
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(86400)
        );
    }

    static void seedIssuedInvoice(
        MockMvc mockMvc,
        PaymentsDeterministicClockTestSupport.AdjustableClock testClock,
        String tenantCode,
        String sku,
        String orderNumber,
        String invoiceNumber,
        Instant dueAt
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
        createInvoice(mockMvc, tenantCode, invoiceNumber, orderNumber, dueAt)
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
