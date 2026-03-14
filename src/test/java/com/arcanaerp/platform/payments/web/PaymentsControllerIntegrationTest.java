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
    private static final String SUMMARY_TENANT_CODE = "tenant-summary";
    private static final String INVOICE_SUMMARY_TENANT_CODE = "tenant-invoice-summary";
    private static final String DAILY_SUMMARY_TENANT_CODE = "tenant-daily-summary";
    private static final String MONTHLY_SUMMARY_TENANT_CODE = "tenant-monthly-summary";
    private static final String WEEKLY_SUMMARY_TENANT_CODE = "tenant-weekly-summary";
    private static final String RECEIVABLES_TENANT_CODE = "tenant-receivables";

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
    void listsTenantReceivablesForIssuedInvoices() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            RECEIVABLES_TENANT_CODE,
            "arc-pay-1015",
            "so-pay-1015",
            "inv-pay-1015"
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            RECEIVABLES_TENANT_CODE,
            "arc-pay-1016",
            "so-pay-1016",
            "inv-pay-1016"
        );

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            RECEIVABLES_TENANT_CODE,
            "pay-1017",
            "inv-pay-1015",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantReceivablesRequest(
                RECEIVABLES_TENANT_CODE,
                "USD",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1015')].tenantCode").value(org.hamcrest.Matchers.hasItem("TENANT-RECEIVABLES")))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1015')].currencyCode").value(org.hamcrest.Matchers.hasItem("USD")))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1015')].paidAmount").value(org.hamcrest.Matchers.hasItem(4.0)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1015')].outstandingAmount").value(org.hamcrest.Matchers.hasItem(6.0)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1016')].totalAmount").value(org.hamcrest.Matchers.hasItem(10.0)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1016')].paidAmount").value(org.hamcrest.Matchers.hasItem(0)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1016')].outstandingAmount").value(org.hamcrest.Matchers.hasItem(10.0)))
            .andExpect(jsonPath("$.items[?(@.invoiceNumber=='INV-PAY-1016')].paidInFull").value(org.hamcrest.Matchers.hasItem(false)));
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

    @Test
    void returnsTenantPaymentSummaryForCurrencyAndDateRange() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1005", "so-pay-1005", "inv-pay-1005");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1006", "so-pay-1006", "inv-pay-1006");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            SUMMARY_TENANT_CODE,
            "pay-1005",
            "inv-pay-1005",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            SUMMARY_TENANT_CODE,
            "pay-1006",
            "inv-pay-1006",
            "5.00",
            "USD",
            PAID_AT.plusSeconds(60)
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantSummaryRequest(
                SUMMARY_TENANT_CODE,
                "USD",
                "paidAtFrom",
                PAID_AT.minusSeconds(1).toString(),
                "paidAtTo",
                PAID_AT.plusSeconds(61).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-SUMMARY"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.paymentCount").value(2))
            .andExpect(jsonPath("$.invoiceCount").value(2))
            .andExpect(jsonPath("$.totalCollected").value(9.0));
    }

    @Test
    void rejectsInvalidTenantSummaryFilters() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantSummaryRequest(
                "tenant-pay",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("currencyCode query parameter must not be blank"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantSummaryRequest(
                "tenant-pay",
                "USD",
                "paidAtFrom",
                "2026-03-13T00:00:00Z",
                "paidAtTo",
                "2026-03-12T00:00:00Z"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("paidAtFrom must be before or equal to paidAtTo"));
    }

    @Test
    void listsTenantInvoicePaymentBreakdown() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1007", "so-pay-1007", "inv-pay-1007");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1008", "so-pay-1008", "inv-pay-1008");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            INVOICE_SUMMARY_TENANT_CODE,
            "pay-1008",
            "inv-pay-1007",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            INVOICE_SUMMARY_TENANT_CODE,
            "pay-1009",
            "inv-pay-1007",
            "2.00",
            "USD",
            PAID_AT.plusSeconds(10)
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            INVOICE_SUMMARY_TENANT_CODE,
            "pay-1010",
            "inv-pay-1008",
            "5.00",
            "USD",
            PAID_AT.plusSeconds(20)
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantInvoiceSummaryRequest(
                INVOICE_SUMMARY_TENANT_CODE,
                "USD",
                0,
                10,
                "paidAtFrom",
                PAID_AT.minusSeconds(1).toString(),
                "paidAtTo",
                PAID_AT.plusSeconds(21).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-INVOICE-SUMMARY"))
            .andExpect(jsonPath("$.items[0].currencyCode").value("USD"))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1007"))
            .andExpect(jsonPath("$.items[0].paymentCount").value(2))
            .andExpect(jsonPath("$.items[0].totalCollected").value(6.0))
            .andExpect(jsonPath("$.items[1].invoiceNumber").value("INV-PAY-1008"))
            .andExpect(jsonPath("$.items[1].paymentCount").value(1))
            .andExpect(jsonPath("$.items[1].totalCollected").value(5.0));
    }

    @Test
    void listsDailyTenantPaymentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1009", "so-pay-1009", "inv-pay-1009");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1010", "so-pay-1010", "inv-pay-1010");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            DAILY_SUMMARY_TENANT_CODE,
            "pay-1011",
            "inv-pay-1009",
            "4.00",
            "USD",
            Instant.parse("2026-03-12T23:30:00Z")
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            DAILY_SUMMARY_TENANT_CODE,
            "pay-1012",
            "inv-pay-1010",
            "5.00",
            "USD",
            Instant.parse("2026-03-13T00:15:00Z")
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.dailyTenantSummaryRequest(
                DAILY_SUMMARY_TENANT_CODE,
                "USD",
                0,
                10,
                "paidAtFrom",
                "2026-03-12T00:00:00Z",
                "paidAtTo",
                "2026-03-13T23:59:59Z"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-DAILY-SUMMARY"))
            .andExpect(jsonPath("$.items[0].currencyCode").value("USD"))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-03-13"))
            .andExpect(jsonPath("$.items[0].paymentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[0].totalCollected").value(5.0))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-03-12"))
            .andExpect(jsonPath("$.items[1].totalCollected").value(4.0));
    }

    @Test
    void listsMonthlyTenantPaymentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1011", "so-pay-1011", "inv-pay-1011");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1012", "so-pay-1012", "inv-pay-1012");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            MONTHLY_SUMMARY_TENANT_CODE,
            "pay-1013",
            "inv-pay-1011",
            "4.00",
            "USD",
            Instant.parse("2026-03-31T23:30:00Z")
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            MONTHLY_SUMMARY_TENANT_CODE,
            "pay-1014",
            "inv-pay-1012",
            "5.00",
            "USD",
            Instant.parse("2026-04-01T00:15:00Z")
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.monthlyTenantSummaryRequest(
                MONTHLY_SUMMARY_TENANT_CODE,
                "USD",
                0,
                10,
                "paidAtFrom",
                "2026-03-01T00:00:00Z",
                "paidAtTo",
                "2026-04-30T23:59:59Z"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-MONTHLY-SUMMARY"))
            .andExpect(jsonPath("$.items[0].currencyCode").value("USD"))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-04"))
            .andExpect(jsonPath("$.items[0].paymentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[0].totalCollected").value(5.0))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-03"))
            .andExpect(jsonPath("$.items[1].totalCollected").value(4.0));
    }

    @Test
    void listsWeeklyTenantPaymentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1013", "so-pay-1013", "inv-pay-1013");
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(mockMvc, testClock, "arc-pay-1014", "so-pay-1014", "inv-pay-1014");

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            WEEKLY_SUMMARY_TENANT_CODE,
            "pay-1015",
            "inv-pay-1013",
            "4.00",
            "USD",
            Instant.parse("2026-04-05T23:30:00Z")
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            WEEKLY_SUMMARY_TENANT_CODE,
            "pay-1016",
            "inv-pay-1014",
            "5.00",
            "USD",
            Instant.parse("2026-04-06T00:15:00Z")
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.weeklyTenantSummaryRequest(
                WEEKLY_SUMMARY_TENANT_CODE,
                "USD",
                0,
                10,
                "paidAtFrom",
                "2026-04-01T00:00:00Z",
                "paidAtTo",
                "2026-04-30T23:59:59Z"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-WEEKLY-SUMMARY"))
            .andExpect(jsonPath("$.items[0].currencyCode").value("USD"))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-04-06"))
            .andExpect(jsonPath("$.items[0].paymentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[0].totalCollected").value(5.0))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-03-30"))
            .andExpect(jsonPath("$.items[1].totalCollected").value(4.0));
    }
}
