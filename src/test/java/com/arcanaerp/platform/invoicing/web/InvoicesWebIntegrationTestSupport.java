package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class InvoicesWebIntegrationTestSupport {

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
        String payload = """
            {
              "status": "%s"
            }
            """.formatted(status);

        return mockMvc.perform(patch("/api/invoices/" + invoiceNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    static MockHttpServletRequestBuilder getInvoiceRequest(String invoiceNumber) {
        return get("/api/invoices/" + invoiceNumber);
    }

    static MockHttpServletRequestBuilder listInvoicesRequest(int page, int size) {
        return get("/api/invoices")
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }
}
