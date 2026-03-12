package com.arcanaerp.platform.invoicing.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.OrderManagementWebTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(InvoicesDeterministicClockTestSupport.Configuration.class)
class InvoicesControllerIntegrationTest {

    private static final Instant DUE_AT = InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(86400);
    private static final Instant ISSUED_AT = InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120);
    private static final Instant VOIDED_AT = InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(180);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoicesDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsAndReadsInvoiceForConfirmedOrder() throws Exception {
        InvoicesWebIntegrationTestSupport.registerProduct(mockMvc, "arc-6101")
            .andExpect(status().isCreated());
        OrderManagementWebTestSupport.createOrder(
            mockMvc,
            "so-6101",
            "buyer@acme.com",
            "USD",
            OrderManagementWebTestSupport.line("arc-6101", "2", "10.00")
        ).andExpect(status().isCreated());
        testClock.setInstant(InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60));
        OrderManagementWebTestSupport.transitionOrderStatus(mockMvc, "so-6101", "CONFIRMED")
            .andExpect(status().isOk());
        testClock.resetToBaseInstant();

        InvoicesWebIntegrationTestSupport.createInvoice(mockMvc, "tenant-inv", "inv-6101", "so-6101", DUE_AT)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-INV"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-6101"))
            .andExpect(jsonPath("$.orderNumber").value("SO-6101"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.totalAmount").value(20.0))
            .andExpect(jsonPath("$.issuedAt").value(nullValue()))
            .andExpect(jsonPath("$.voidedAt").value(nullValue()))
            .andExpect(jsonPath("$.lines[0].lineNo").value(1))
            .andExpect(jsonPath("$.lines[0].productSku").value("ARC-6101"))
            .andExpect(jsonPath("$.lines[0].quantity").value(2.0))
            .andExpect(jsonPath("$.lines[0].unitPrice").value(10.0))
            .andExpect(jsonPath("$.lines[0].lineTotal").value(20.0));

        mockMvc.perform(InvoicesWebIntegrationTestSupport.getInvoiceRequest("inv-6101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-6101"))
            .andExpect(jsonPath("$.orderNumber").value("SO-6101"))
            .andExpect(jsonPath("$.lines[0].productSku").value("ARC-6101"));

        mockMvc.perform(InvoicesWebIntegrationTestSupport.listInvoicesRequest(0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-6101')].tenantCode", hasItem("TENANT-INV")));

        testClock.setInstant(ISSUED_AT);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-6101", "ISSUED")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"))
            .andExpect(jsonPath("$.issuedAt").value(ISSUED_AT.toString()))
            .andExpect(jsonPath("$.voidedAt").value(nullValue()));
    }

    @Test
    void rejectsInvoiceCreationForNonConfirmedOrder() throws Exception {
        InvoicesWebIntegrationTestSupport.registerProduct(mockMvc, "arc-6102")
            .andExpect(status().isCreated());
        OrderManagementWebTestSupport.createSingleLineOrder(
            mockMvc,
            "so-6102",
            "buyer@acme.com",
            "arc-6102",
            "1",
            "10.00",
            "USD"
        ).andExpect(status().isCreated());

        InvoicesWebIntegrationTestSupport.createInvoice(mockMvc, "tenant-inv", "inv-6102", "so-6102", DUE_AT)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Order must be CONFIRMED before invoicing: SO-6102"))
            .andExpect(jsonPath("$.path").value("/api/invoices"));
    }

    @Test
    void returnsNotFoundForUnknownInvoice() throws Exception {
        mockMvc.perform(InvoicesWebIntegrationTestSupport.getInvoiceRequest("inv-missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Invoice not found: INV-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/invoices/inv-missing"));
    }

    @Test
    void listsInvoiceStatusHistoryAndIgnoresNoOpTransitions() throws Exception {
        seedConfirmedOrderAndInvoice("arc-6103", "so-6103", "inv-6103");

        testClock.setInstant(ISSUED_AT);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-6103", "ISSUED")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"));

        testClock.setInstant(ISSUED_AT.plusSeconds(60));
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-6103", "ISSUED")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"));

        mockMvc.perform(InvoicesStatusHistoryWebTestSupport.statusHistoryRequestDefault("inv-6103"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-6103"))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ISSUED"))
            .andExpect(jsonPath("$.items[0].changedAt").value(ISSUED_AT.toString()));
    }

    @Test
    void statusHistoryReturnsNotFoundForUnknownInvoice() throws Exception {
        mockMvc.perform(InvoicesStatusHistoryWebTestSupport.statusHistoryRequestDefault("inv-missing-history"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Invoice not found: INV-MISSING-HISTORY"))
            .andExpect(jsonPath("$.path").value("/api/invoices/inv-missing-history/status-history"));
    }

    @Test
    void filtersInvoiceStatusHistoryByStatusAndChangedAtRange() throws Exception {
        seedConfirmedOrderAndInvoice("arc-6104", "so-6104", "inv-6104");

        testClock.setInstant(ISSUED_AT);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-6104", "ISSUED")
            .andExpect(status().isOk());

        testClock.setInstant(VOIDED_AT);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-6104", "VOID")
            .andExpect(status().isOk());

        mockMvc.perform(InvoicesStatusHistoryWebTestSupport.statusHistoryRequestDefault(
                "inv-6104",
                "currentStatus",
                "VOID",
                "changedAtFrom",
                VOIDED_AT.minusSeconds(1).toString(),
                "changedAtTo",
                VOIDED_AT.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("ISSUED"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("VOID"));
    }

    @Test
    void rejectsInvalidInvoiceStatusHistoryFilters() throws Exception {
        mockMvc.perform(InvoicesStatusHistoryWebTestSupport.statusHistoryRequestDefault(
                "inv-invalid-history",
                "previousStatus",
                "invalid"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("previousStatus query parameter must be one of: DRAFT, ISSUED, VOID"))
            .andExpect(jsonPath("$.path").value("/api/invoices/inv-invalid-history/status-history"));

        mockMvc.perform(InvoicesStatusHistoryWebTestSupport.statusHistoryRequestDefault(
                "inv-invalid-range",
                "changedAtFrom",
                "2026-03-12T00:00:00Z",
                "changedAtTo",
                "2026-03-11T00:00:00Z"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"));
    }

    private void seedConfirmedOrderAndInvoice(String sku, String orderNumber, String invoiceNumber) throws Exception {
        InvoicesWebIntegrationTestSupport.registerProduct(mockMvc, sku)
            .andExpect(status().isCreated());
        OrderManagementWebTestSupport.createSingleLineOrder(
            mockMvc,
            orderNumber,
            "buyer@acme.com",
            sku,
            "1",
            "10.00",
            "USD"
        ).andExpect(status().isCreated());
        testClock.setInstant(InvoicesDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60));
        OrderManagementWebTestSupport.transitionOrderStatus(mockMvc, orderNumber, "CONFIRMED")
            .andExpect(status().isOk());
        testClock.resetToBaseInstant();
        InvoicesWebIntegrationTestSupport.createInvoice(mockMvc, "tenant-inv", invoiceNumber, orderNumber, DUE_AT)
            .andExpect(status().isCreated());
    }
}
