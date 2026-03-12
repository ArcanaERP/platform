package com.arcanaerp.platform.payments.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@Import(PaymentsDeterministicClockTestSupport.Configuration.class)
class PaymentsControllerIntegrationTest {

    private static final Instant PAID_AT = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(180);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentsDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsPaymentAndReturnsInvoiceBalance() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1000", "so-pay-1000", "inv-pay-1000");

        testClock.setInstant(PAID_AT);
        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            "tenant-pay",
            "pay-1000",
            "inv-pay-1000",
            "4.00",
            "USD",
            PAID_AT
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-PAY"))
            .andExpect(jsonPath("$.paymentReference").value("PAY-1000"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-PAY-1000"))
            .andExpect(jsonPath("$.amount").value(4.0))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.paidAt").value(PAID_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(PAID_AT.toString()));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.invoiceBalanceRequest("inv-pay-1000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-PAY-1000"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.totalAmount").value(10.0))
            .andExpect(jsonPath("$.paidAmount").value(4.0))
            .andExpect(jsonPath("$.outstandingAmount").value(6.0))
            .andExpect(jsonPath("$.paidInFull").value(false));
    }

    @Test
    void rejectsPaymentForNonIssuedInvoice() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1001", "so-pay-1001", "inv-pay-1001");
        PaymentsWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, "inv-pay-1001", "VOID")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VOID"));

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            "tenant-pay",
            "pay-1001",
            "inv-pay-1001",
            "4.00",
            "USD",
            PAID_AT
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invoice must be ISSUED before payment: INV-PAY-1001"))
            .andExpect(jsonPath("$.path").value("/api/payments"));
    }

    @Test
    void rejectsPaymentThatExceedsOutstandingBalance() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1002", "so-pay-1002", "inv-pay-1002");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            "tenant-pay",
            "pay-1002",
            "inv-pay-1002",
            "11.00",
            "USD",
            PAID_AT
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Payment amount exceeds outstanding invoice balance: INV-PAY-1002"));
    }

    @Test
    void listsPaymentsWithInvoiceAndTenantFilters() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1003", "so-pay-1003", "inv-pay-1003");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1004", "so-pay-1004", "inv-pay-1004");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            "tenant-pay",
            "pay-1003",
            "inv-pay-1003",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            "tenant-alt",
            "pay-1004",
            "inv-pay-1004",
            "5.00",
            "USD",
            PAID_AT.plusSeconds(60)
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.listPaymentsRequest(
                0,
                10,
                "invoiceNumber",
                "inv-pay-1003",
                "tenantCode",
                "tenant-pay",
                "paidAtFrom",
                PAID_AT.minusSeconds(1).toString(),
                "paidAtTo",
                PAID_AT.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].paymentReference").value("PAY-1003"))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1003"))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-PAY"));
    }

    @Test
    void rejectsInvalidPaymentListFilters() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.listPaymentsRequest(
                0,
                10,
                "invoiceNumber",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invoiceNumber query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/payments"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.listPaymentsRequest(
                0,
                10,
                "paidAtFrom",
                "2026-03-13T00:00:00Z",
                "paidAtTo",
                "2026-03-12T00:00:00Z"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("paidAtFrom must be before or equal to paidAtTo"));
    }
}
