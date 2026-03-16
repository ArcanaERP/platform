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
    private static final String AGING_TENANT_CODE = "tenant-aging";
    private static final String AGING_BUCKET_TENANT_CODE = "tenant-aging-bucket";
    private static final String COLLECTIONS_TENANT_CODE = "tenant-collections";
    private static final String COLLECTIONS_ASSIGNMENT_TENANT_CODE = "tenant-collections-assignment";
    private static final String COLLECTIONS_ASSIGNMENT_MISSING_TENANT_CODE = "tenant-coll-assign-miss";
    private static final String COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE = "tenant-coll-assign-history";
    private static final String COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE = "tenant-coll-assignee-filter";
    private static final String COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE = "tenant-coll-assign-histflt";
    private static final String COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE = "tenant-coll-assign-feed";
    private static final String COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE = "tenant-coll-assign-sum";
    private static final String COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE = "tenant-coll-assign-day";
    private static final String COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE = "tenant-coll-assign-week";
    private static final String COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE = "tenant-coll-assign-month";
    private static final String COLLECTIONS_NOTES_TENANT_CODE = "tenant-coll-notes";
    private static final String COLLECTIONS_NOTES_FEED_TENANT_CODE = "tenant-coll-notes-feed";
    private static final String COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE = "tenant-coll-note-outsum";
    private static final String COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE = "tenant-coll-note-catsum";
    private static final String COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE = "tenant-coll-note-catday";
    private static final String COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE = "tenant-coll-note-catweek";
    private static final String COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE = "tenant-coll-note-assignee";
    private static final String COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE = "tenant-coll-note-daysum";
    private static final String COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE = "tenant-coll-note-weeksum";
    private static final String COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE = "tenant-coll-note-monthsum";
    private static final String COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE = "tenant-coll-note-outday";
    private static final String COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE = "tenant-coll-note-outweek";
    private static final String COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE = "tenant-coll-note-outmonth";

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
    void returnsTenantReceivablesSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            RECEIVABLES_TENANT_CODE,
            "arc-pay-1017",
            "so-pay-1017",
            "inv-pay-1017"
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            RECEIVABLES_TENANT_CODE,
            "arc-pay-1018",
            "so-pay-1018",
            "inv-pay-1018"
        );

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            RECEIVABLES_TENANT_CODE,
            "pay-1018",
            "inv-pay-1017",
            "10.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            RECEIVABLES_TENANT_CODE,
            "pay-1019",
            "inv-pay-1018",
            "4.00",
            "USD",
            PAID_AT.plusSeconds(60)
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantReceivablesSummaryRequest(
                RECEIVABLES_TENANT_CODE,
                "USD"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-RECEIVABLES"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.invoiceCount").value(4))
            .andExpect(jsonPath("$.totalAmount").value(40.0))
            .andExpect(jsonPath("$.paidAmount").value(18.0))
            .andExpect(jsonPath("$.outstandingAmount").value(22.0))
            .andExpect(jsonPath("$.paidInFullCount").value(1));
    }

    @Test
    void returnsTenantReceivablesAging() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_TENANT_CODE,
            "arc-pay-1020",
            "so-pay-1020",
            "inv-pay-1020",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(132 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_TENANT_CODE,
            "arc-pay-1021",
            "so-pay-1021",
            "inv-pay-1021",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_TENANT_CODE,
            "arc-pay-1022",
            "so-pay-1022",
            "inv-pay-1022",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(85 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_TENANT_CODE,
            "arc-pay-1023",
            "so-pay-1023",
            "inv-pay-1023",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(55 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_TENANT_CODE,
            "arc-pay-1024",
            "so-pay-1024",
            "inv-pay-1024",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            AGING_TENANT_CODE,
            "pay-1020",
            "inv-pay-1021",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            AGING_TENANT_CODE,
            "pay-1021",
            "inv-pay-1024",
            "10.00",
            "USD",
            PAID_AT.plusSeconds(60)
        ).andExpect(status().isCreated());

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantReceivablesAgingRequest(
                AGING_TENANT_CODE,
                "USD"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-AGING"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.asOfDate").value("2026-07-20"))
            .andExpect(jsonPath("$.totalOutstandingInvoiceCount").value(4))
            .andExpect(jsonPath("$.totalOutstandingAmount").value(36.0))
            .andExpect(jsonPath("$.currentInvoiceCount").value(1))
            .andExpect(jsonPath("$.currentAmount").value(10.0))
            .andExpect(jsonPath("$.overdue1To30InvoiceCount").value(1))
            .andExpect(jsonPath("$.overdue1To30Amount").value(6.0))
            .andExpect(jsonPath("$.overdue31To60InvoiceCount").value(1))
            .andExpect(jsonPath("$.overdue31To60Amount").value(10.0))
            .andExpect(jsonPath("$.overdue61To90InvoiceCount").value(1))
            .andExpect(jsonPath("$.overdue61To90Amount").value(10.0))
            .andExpect(jsonPath("$.overdueOver90InvoiceCount").value(0))
            .andExpect(jsonPath("$.overdueOver90Amount").value(0.0));
    }

    @Test
    void listsTenantReceivablesForAgingBucket() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_BUCKET_TENANT_CODE,
            "arc-pay-1025",
            "so-pay-1025",
            "inv-pay-1025",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_BUCKET_TENANT_CODE,
            "arc-pay-1026",
            "so-pay-1026",
            "inv-pay-1026",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(110 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            AGING_BUCKET_TENANT_CODE,
            "arc-pay-1027",
            "so-pay-1027",
            "inv-pay-1027",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(90 * 86400)
        );

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            AGING_BUCKET_TENANT_CODE,
            "pay-1025",
            "inv-pay-1027",
            "10.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantReceivablesByAgingBucketRequest(
                AGING_BUCKET_TENANT_CODE,
                "OVERDUE_1_TO_30",
                "USD",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1026"))
            .andExpect(jsonPath("$.items[0].daysPastDue").value(20))
            .andExpect(jsonPath("$.items[0].agingBucket").value("OVERDUE_1_TO_30"))
            .andExpect(jsonPath("$.items[0].outstandingAmount").value(10.0))
            .andExpect(jsonPath("$.items[1].invoiceNumber").value("INV-PAY-1025"))
            .andExpect(jsonPath("$.items[1].daysPastDue").value(10))
            .andExpect(jsonPath("$.items[1].agingBucket").value("OVERDUE_1_TO_30"))
            .andExpect(jsonPath("$.items[1].outstandingAmount").value(10.0));
    }

    @Test
    void rejectsUnsupportedReceivablesAgingBucket() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantReceivablesByAgingBucketRequest(
                AGING_BUCKET_TENANT_CODE,
                "stale",
                "USD",
                0,
                10
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported agingBucket: STALE"));
    }

    @Test
    void listsOver90CollectionsQueueAndSupportsInvoiceFilter() throws Exception {
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_TENANT_CODE,
            "arc-pay-1028",
            "so-pay-1028",
            "inv-pay-1028",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_TENANT_CODE,
            "arc-pay-1029",
            "so-pay-1029",
            "inv-pay-1029",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_TENANT_CODE,
            "arc-pay-1030",
            "so-pay-1030",
            "inv-pay-1030",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120 * 86400)
        );

        PaymentsWebIntegrationTestSupport.createPayment(
            mockMvc,
            COLLECTIONS_TENANT_CODE,
            "pay-1028",
            "inv-pay-1029",
            "4.00",
            "USD",
            PAID_AT
        ).andExpect(status().isCreated());

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_TENANT_CODE,
                "USD",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1028"))
            .andExpect(jsonPath("$.items[0].daysPastDue").value(120))
            .andExpect(jsonPath("$.items[1].invoiceNumber").value("INV-PAY-1029"))
            .andExpect(jsonPath("$.items[1].daysPastDue").value(115))
            .andExpect(jsonPath("$.items[1].outstandingAmount").value(6.0));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_TENANT_CODE,
                "USD",
                0,
                10,
                "invoiceNumber",
                "inv-pay-1029"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1029"))
            .andExpect(jsonPath("$.items[0].agingBucket").value("OVERDUE_OVER_90"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_TENANT_CODE,
                "USD",
                0,
                10,
                "dueAtOnOrBefore",
                PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(12 * 86400).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1028"));
    }

    @Test
    void rejectsBlankOver90CollectionsInvoiceFilter() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_TENANT_CODE,
                "USD",
                0,
                10,
                "invoiceNumber",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invoiceNumber query parameter must not be blank"));
    }

    @Test
    void rejectsInvalidOver90CollectionsDueAtCutoff() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_TENANT_CODE,
                "USD",
                0,
                10,
                "dueAtOnOrBefore",
                "not-an-instant"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("dueAtOnOrBefore query parameter must be a valid ISO-8601 instant"));
    }

    @Test
    void assignsOver90CollectionsInvoiceAndExposesAssignmentOnQueue() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_CODE,
            "Collections Tenant",
            "COLLECTOR",
            "Collector",
            "collector@arcanaerp.com",
            "Collector"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_CODE,
            "Collections Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_TENANT_CODE,
            "arc-pay-1031",
            "so-pay-1031",
            "inv-pay-1031",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_CODE,
            "inv-pay-1031",
            "collector@arcanaerp.com",
            "manager@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-COLLECTIONS-ASSIGNMENT"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-PAY-1031"))
            .andExpect(jsonPath("$.assignedTo").value("collector@arcanaerp.com"))
            .andExpect(jsonPath("$.assignedBy").value("manager@arcanaerp.com"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_ASSIGNMENT_TENANT_CODE,
                "USD",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1031"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedBy").value("manager@arcanaerp.com"));
    }

    @Test
    void filtersOver90CollectionsQueueByAssignedTo() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "Collections Filter Tenant",
            "COLLECTOR",
            "Collector",
            "collector@arcanaerp.com",
            "Collector"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "Collections Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "Collections Filter Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "arc-pay-1031b",
            "so-pay-1031b",
            "inv-pay-1031b",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "arc-pay-1034",
            "so-pay-1034",
            "inv-pay-1034",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "inv-pay-1031b",
            "collector@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
            "inv-pay-1034",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
                "USD",
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1034"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-b@arcanaerp.com"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
                "USD",
                0,
                10,
                "assignedTo",
                "collector@arcanaerp.com"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1031B"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector@arcanaerp.com"));
    }

    @Test
    void rejectsCollectionsAssignmentForMissingActor() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MISSING_TENANT_CODE,
            "Collections Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_MISSING_TENANT_CODE,
            "arc-pay-1032",
            "so-pay-1032",
            "inv-pay-1032",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MISSING_TENANT_CODE,
            "inv-pay-1032",
            "collector@arcanaerp.com",
            "manager@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Collections assignee not found in tenant TENANT-COLL-ASSIGN-MISS: collector@arcanaerp.com"
            ));
    }

    @Test
    void createsAndListsCollectionsNotesNewestFirst() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "Collections Notes Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "Collections Notes Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "Collections Notes Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_TENANT_CODE,
            "arc-pay-1050",
            "so-pay-1050",
            "inv-pay-1050",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        Instant assignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(assignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "inv-pay-1050",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "inv-pay-1050",
            "Called customer, promised payment next week.",
            "collector-a@arcanaerp.com",
            "PAYMENT_PROMISE",
            "PROMISE_TO_PAY"
        ).andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-COLL-NOTES"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-PAY-1050"))
            .andExpect(jsonPath("$.note").value("Called customer, promised payment next week."))
            .andExpect(jsonPath("$.notedBy").value("collector-a@arcanaerp.com"))
            .andExpect(jsonPath("$.category").value("PAYMENT_PROMISE"))
            .andExpect(jsonPath("$.outcome").value("PROMISE_TO_PAY"))
            .andExpect(jsonPath("$.notedAt").value(assignedAt.toString()));

        Instant secondNoteAt = assignedAt.plusSeconds(60);
        testClock.setInstant(secondNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "inv-pay-1050",
            "Escalated to collector B for follow-up.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated())
            .andExpect(jsonPath("$.notedAt").value(secondNoteAt.toString()));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsNotesRequest(
                COLLECTIONS_NOTES_TENANT_CODE,
                "inv-pay-1050",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].note").value("Escalated to collector B for follow-up."))
            .andExpect(jsonPath("$.items[0].notedBy").value("collector-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].category").value("ESCALATION"))
            .andExpect(jsonPath("$.items[0].outcome").value("ESCALATED"))
            .andExpect(jsonPath("$.items[1].note").value("Called customer, promised payment next week."))
            .andExpect(jsonPath("$.items[1].notedBy").value("collector-a@arcanaerp.com"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsNotesRequest(
                COLLECTIONS_NOTES_TENANT_CODE,
                "inv-pay-1050",
                0,
                10,
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "ESCALATION",
                "outcome",
                "ESCALATED",
                "notedAtFrom",
                secondNoteAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondNoteAt.toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].note").value("Escalated to collector B for follow-up."));
    }

    @Test
    void rejectsCollectionsNotesForInvalidFiltersAndUnassignedInvoices() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsNotesRequest(
                COLLECTIONS_NOTES_TENANT_CODE,
                "inv-pay-1050",
                0,
                10,
                "notedBy",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("notedBy query parameter must not be blank"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsNotesRequest(
                COLLECTIONS_NOTES_TENANT_CODE,
                "inv-pay-1050",
                0,
                10,
                "notedAtFrom",
                Instant.parse("2026-03-12T00:01:00Z").toString(),
                "notedAtTo",
                Instant.parse("2026-03-12T00:00:00Z").toString()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("notedAtFrom must be before or equal to notedAtTo"));

        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "Collections Notes Tenant",
            "COLLECTOR",
            "Collector",
            "collector-c@arcanaerp.com",
            "Collector C"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_TENANT_CODE,
            "arc-pay-1051",
            "so-pay-1051",
            "inv-pay-1051",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_TENANT_CODE,
            "inv-pay-1051",
            "Attempted note without assignment.",
            "collector-c@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "NO_CONTACT"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Invoice is not currently assigned for collections notes: INV-PAY-1051"
            ));
    }

    @Test
    void listsTenantCollectionsNotesWithFilters() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "Collections Notes Feed Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "Collections Notes Feed Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "Collections Notes Feed Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "arc-pay-1052",
            "so-pay-1052",
            "inv-pay-1052",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "arc-pay-1053",
            "so-pay-1053",
            "inv-pay-1053",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstAssignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(firstAssignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "inv-pay-1052",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "inv-pay-1053",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "inv-pay-1052",
            "Left voicemail for AP team.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "NO_CONTACT"
        ).andExpect(status().isCreated());

        Instant secondNoteAt = firstAssignedAt.plusSeconds(60);
        testClock.setInstant(secondNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_FEED_TENANT_CODE,
            "inv-pay-1053",
            "Confirmed dispute review is underway.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNotesRequest(
                COLLECTIONS_NOTES_FEED_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1053"))
            .andExpect(jsonPath("$.items[0].notedBy").value("collector-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].invoiceNumber").value("INV-PAY-1052"))
            .andExpect(jsonPath("$.items[1].notedBy").value("collector-a@arcanaerp.com"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNotesRequest(
                COLLECTIONS_NOTES_FEED_TENANT_CODE,
                0,
                10,
                "invoiceNumber",
                "inv-pay-1053",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondNoteAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondNoteAt.toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1053"))
            .andExpect(jsonPath("$.items[0].note").value("Confirmed dispute review is underway."));
    }

    @Test
    void listsTenantCollectionsNotesWithAssignedToFilter() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "Collections Notes Assignee Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "Collections Notes Assignee Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "Collections Notes Assignee Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());

        Instant assignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400L);
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "arc-pay-1058",
            "so-pay-1058",
            "inv-pay-1058",
            assignedAt.minusSeconds(100L * 24L * 60L * 60L)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "arc-pay-1059",
            "so-pay-1059",
            "inv-pay-1059",
            assignedAt.minusSeconds(100L * 24L * 60L * 60L)
        );

        testClock.setInstant(assignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "inv-pay-1058",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "inv-pay-1059",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "inv-pay-1058",
            "Collector A note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        testClock.setInstant(assignedAt.plusSeconds(30));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
            "inv-pay-1059",
            "Collector B note.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNotesRequest(
                COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1059"))
            .andExpect(jsonPath("$.items[0].notedBy").value("collector-b@arcanaerp.com"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNotesRequest(
                COLLECTIONS_NOTES_ASSIGNEE_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsTenantCollectionsNoteOutcomeSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "Collections Notes Outcome Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "Collections Notes Outcome Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "Collections Notes Outcome Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "arc-pay-1054",
            "so-pay-1054",
            "inv-pay-1054",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "arc-pay-1055",
            "so-pay-1055",
            "inv-pay-1055",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant assignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(assignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "inv-pay-1054",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "inv-pay-1055",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "inv-pay-1054",
            "Promise captured.",
            "collector-a@arcanaerp.com",
            "PAYMENT_PROMISE",
            "PROMISE_TO_PAY"
        ).andExpect(status().isCreated());

        Instant secondNoteAt = assignedAt.plusSeconds(1);
        testClock.setInstant(secondNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "inv-pay-1055",
            "Dispute opened.",
            "collector-a@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        Instant thirdNoteAt = assignedAt.plusSeconds(60);
        testClock.setInstant(thirdNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
            "inv-pay-1055",
            "Escalated for follow-up.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeSummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].outcome").value("ESCALATED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[2].outcome").value("PROMISE_TO_PAY"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeSummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-a@arcanaerp.com",
                "category",
                "DISPUTE",
                "notedAtFrom",
                secondNoteAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondNoteAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeSummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeSummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_SUM_TENANT_CODE,
                0,
                10,
                "notedBy",
                "collector-a@arcanaerp.com",
                "notedAtFrom",
                assignedAt.minusSeconds(1).toString(),
                "notedAtTo",
                assignedAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[1].outcome").value("PROMISE_TO_PAY"));
    }

    @Test
    void listsTenantCollectionsNoteCategorySummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "Collections Note Category Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "Collections Note Category Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "Collections Note Category Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());

        Instant assignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130L * 24L * 60L * 60L);
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "arc-pay-1056",
            "so-pay-1056",
            "inv-pay-1056",
            assignedAt.minusSeconds(100L * 24L * 60L * 60L)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "arc-pay-1057",
            "so-pay-1057",
            "inv-pay-1057",
            assignedAt.minusSeconds(100L * 24L * 60L * 60L)
        );

        testClock.setInstant(assignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "inv-pay-1056",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "inv-pay-1057",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "inv-pay-1056",
            "Attempted outreach.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "NO_CONTACT"
        ).andExpect(status().isCreated());

        Instant secondNoteAt = assignedAt.plusSeconds(1);
        testClock.setInstant(secondNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "inv-pay-1057",
            "Customer opened dispute.",
            "collector-a@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        Instant thirdNoteAt = assignedAt.plusSeconds(60);
        testClock.setInstant(thirdNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
            "inv-pay-1057",
            "Escalated to supervisor.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategorySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].category").value("ESCALATION"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[2].category").value("CONTACT_ATTEMPT"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategorySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-a@arcanaerp.com",
                "notedBy",
                "collector-a@arcanaerp.com",
                "outcome",
                "NO_CONTACT",
                "notedAtFrom",
                assignedAt.minusSeconds(1).toString(),
                "notedAtTo",
                assignedAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].category").value("CONTACT_ATTEMPT"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategorySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_SUM_TENANT_CODE,
                0,
                10,
                "notedBy",
                "collector-a@arcanaerp.com",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondNoteAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondNoteAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));
    }

    @Test
    void listsDailyTenantCollectionsNoteCategorySummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "Collections Note Category Day Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "Collections Note Category Day Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "Collections Note Category Day Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "arc-pay-1090",
            "so-pay-1090",
            "inv-pay-1090",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "arc-pay-1091",
            "so-pay-1091",
            "inv-pay-1091",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstDay = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400L);
        testClock.setInstant(firstDay);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "inv-pay-1090",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "inv-pay-1091",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "inv-pay-1090",
            "Collector A attempted contact.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "NO_CONTACT"
        ).andExpect(status().isCreated());

        Instant secondDay = firstDay.plusSeconds(86400);
        testClock.setInstant(secondDay);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "inv-pay-1091",
            "Collector B opened a dispute.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());
        testClock.setInstant(secondDay.plusSeconds(1));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
            "inv-pay-1091",
            "Collector B escalated the dispute.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryDailySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].category").value("ESCALATION"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[1].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[2].businessDate").value(firstDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[2].category").value("CONTACT_ATTEMPT"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryDailySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondDay.minusSeconds(1).toString(),
                "notedAtTo",
                secondDay.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryDailySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_DAY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsWeeklyTenantCollectionsNoteCategorySummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "Collections Note Category Week Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "Collections Note Category Week Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "Collections Note Category Week Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "arc-pay-1092",
            "so-pay-1092",
            "inv-pay-1092",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "arc-pay-1093",
            "so-pay-1093",
            "inv-pay-1093",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstWeekNoteAt = Instant.parse("2026-07-07T11:00:00Z");
        testClock.setInstant(firstWeekNoteAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "inv-pay-1092",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "inv-pay-1093",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "inv-pay-1092",
            "Collector A attempted contact.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "NO_CONTACT"
        ).andExpect(status().isCreated());

        Instant secondWeekNoteAt = Instant.parse("2026-07-14T12:00:00Z");
        testClock.setInstant(secondWeekNoteAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "inv-pay-1093",
            "Collector B opened a dispute.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());
        testClock.setInstant(secondWeekNoteAt.plusSeconds(1));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
            "inv-pay-1093",
            "Collector B escalated the dispute.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryWeeklySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].category").value("ESCALATION"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[1].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[2].businessWeekStart").value("2026-07-06"))
            .andExpect(jsonPath("$.items[2].category").value("CONTACT_ATTEMPT"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryWeeklySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondWeekNoteAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondWeekNoteAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].category").value("DISPUTE"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteCategoryWeeklySummaryRequest(
                COLLECTIONS_NOTES_CATEGORY_WEEK_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsDailyTenantCollectionsNoteSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "Collections Note Daily Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "Collections Note Daily Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "Collections Note Daily Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "arc-pay-1060",
            "so-pay-1060",
            "inv-pay-1060",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "arc-pay-1061",
            "so-pay-1061",
            "inv-pay-1061",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstDay = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400L);
        testClock.setInstant(firstDay);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "inv-pay-1060",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "inv-pay-1061",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "inv-pay-1060",
            "Collector A day one note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondDay = firstDay.plusSeconds(86400);
        testClock.setInstant(secondDay);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
            "inv-pay-1061",
            "Collector B day two note.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteDailySummaryRequest(
                COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value(firstDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteDailySummaryRequest(
                COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondDay.minusSeconds(1).toString(),
                "notedAtTo",
                secondDay.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteDailySummaryRequest(
                COLLECTIONS_NOTES_DAILY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsWeeklyTenantCollectionsNoteSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "Collections Note Weekly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "Collections Note Weekly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "Collections Note Weekly Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "arc-pay-1062",
            "so-pay-1062",
            "inv-pay-1062",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "arc-pay-1063",
            "so-pay-1063",
            "inv-pay-1063",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstWeek = Instant.parse("2026-07-06T12:00:00Z");
        testClock.setInstant(firstWeek);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "inv-pay-1062",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "inv-pay-1063",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "inv-pay-1062",
            "Collector A week one note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondWeek = Instant.parse("2026-07-13T12:00:00Z");
        testClock.setInstant(secondWeek);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
            "inv-pay-1063",
            "Collector B week two note.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteWeeklySummaryRequest(
                COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-07-06"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteWeeklySummaryRequest(
                COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondWeek.minusSeconds(1).toString(),
                "notedAtTo",
                secondWeek.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteWeeklySummaryRequest(
                COLLECTIONS_NOTES_WEEKLY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsMonthlyTenantCollectionsNoteSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "Collections Note Monthly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "Collections Note Monthly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "Collections Note Monthly Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "arc-pay-1064",
            "so-pay-1064",
            "inv-pay-1064",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "arc-pay-1065",
            "so-pay-1065",
            "inv-pay-1065",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstMonth = Instant.parse("2026-07-15T12:00:00Z");
        testClock.setInstant(firstMonth);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "inv-pay-1064",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "inv-pay-1065",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "inv-pay-1064",
            "Collector A month one note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondMonth = Instant.parse("2026-08-15T12:00:00Z");
        testClock.setInstant(secondMonth);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
            "inv-pay-1065",
            "Collector B month two note.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteMonthlySummaryRequest(
                COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-07"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteMonthlySummaryRequest(
                COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "outcome",
                "DISPUTE_OPENED",
                "notedAtFrom",
                secondMonth.minusSeconds(1).toString(),
                "notedAtTo",
                secondMonth.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteMonthlySummaryRequest(
                COLLECTIONS_NOTES_MONTHLY_SUM_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsDailyTenantCollectionsNoteOutcomeSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "Collections Note Outcome Day Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "Collections Note Outcome Day Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "Collections Note Outcome Day Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "arc-pay-1080",
            "so-pay-1080",
            "inv-pay-1080",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "arc-pay-1081",
            "so-pay-1081",
            "inv-pay-1081",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstDay = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400L);
        testClock.setInstant(firstDay);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "inv-pay-1080",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "inv-pay-1081",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "inv-pay-1080",
            "Collector A day one contact note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondDay = firstDay.plusSeconds(86400);
        testClock.setInstant(secondDay);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "inv-pay-1081",
            "Collector B dispute note.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());
        testClock.setInstant(secondDay.plusSeconds(1));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
            "inv-pay-1081",
            "Collector B escalated the dispute.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeDailySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].outcome").value("ESCALATED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[1].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[2].businessDate").value(firstDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[2].outcome").value("AWAITING_RESPONSE"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeDailySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "notedAtFrom",
                secondDay.minusSeconds(1).toString(),
                "notedAtTo",
                secondDay.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeDailySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_DAY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsWeeklyTenantCollectionsNoteOutcomeSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "Collections Note Outcome Week Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "Collections Note Outcome Week Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "Collections Note Outcome Week Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "arc-pay-1082",
            "so-pay-1082",
            "inv-pay-1082",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "arc-pay-1083",
            "so-pay-1083",
            "inv-pay-1083",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstWeekNoteAt = Instant.parse("2026-07-07T11:00:00Z");
        testClock.setInstant(firstWeekNoteAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "inv-pay-1082",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "inv-pay-1083",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "inv-pay-1082",
            "Collector A first-week contact note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondWeekDisputeAt = Instant.parse("2026-07-14T12:00:00Z");
        testClock.setInstant(secondWeekDisputeAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "inv-pay-1083",
            "Collector B opened a dispute.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());
        testClock.setInstant(secondWeekDisputeAt.plusSeconds(1));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
            "inv-pay-1083",
            "Collector B escalated the dispute.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeWeeklySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].outcome").value("ESCALATED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[1].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[2].businessWeekStart").value("2026-07-06"))
            .andExpect(jsonPath("$.items[2].outcome").value("AWAITING_RESPONSE"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeWeeklySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "notedAtFrom",
                secondWeekDisputeAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondWeekDisputeAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-07-13"))
            .andExpect(jsonPath("$.items[0].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeWeeklySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_WEEK_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsMonthlyTenantCollectionsNoteOutcomeSummaries() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "Collections Note Outcome Month Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "Collections Note Outcome Month Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "Collections Note Outcome Month Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "arc-pay-1084",
            "so-pay-1084",
            "inv-pay-1084",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "arc-pay-1085",
            "so-pay-1085",
            "inv-pay-1085",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );

        Instant firstMonthNoteAt = Instant.parse("2026-07-10T10:00:00Z");
        testClock.setInstant(firstMonthNoteAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "inv-pay-1084",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "inv-pay-1085",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "inv-pay-1084",
            "Collector A first-month contact note.",
            "collector-a@arcanaerp.com",
            "CONTACT_ATTEMPT",
            "AWAITING_RESPONSE"
        ).andExpect(status().isCreated());

        Instant secondMonthDisputeAt = Instant.parse("2026-08-12T12:00:00Z");
        testClock.setInstant(secondMonthDisputeAt);
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "inv-pay-1085",
            "Collector B opened an August dispute.",
            "collector-b@arcanaerp.com",
            "DISPUTE",
            "DISPUTE_OPENED"
        ).andExpect(status().isCreated());
        testClock.setInstant(secondMonthDisputeAt.plusSeconds(1));
        PaymentsWebIntegrationTestSupport.addCollectionsNote(
            mockMvc,
            COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
            "inv-pay-1085",
            "Collector B escalated the August dispute.",
            "collector-b@arcanaerp.com",
            "ESCALATION",
            "ESCALATED"
        ).andExpect(status().isCreated());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeMonthlySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[0].outcome").value("ESCALATED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[1].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[2].businessMonth").value("2026-07"))
            .andExpect(jsonPath("$.items[2].outcome").value("AWAITING_RESPONSE"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeMonthlySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "notedBy",
                "collector-b@arcanaerp.com",
                "category",
                "DISPUTE",
                "notedAtFrom",
                secondMonthDisputeAt.minusSeconds(1).toString(),
                "notedAtTo",
                secondMonthDisputeAt.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[0].outcome").value("DISPUTE_OPENED"))
            .andExpect(jsonPath("$.items[0].noteCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsNoteOutcomeMonthlySummaryRequest(
                COLLECTIONS_NOTES_OUTCOME_MONTH_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void rejectsBlankOver90CollectionsAssignedToFilter() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.over90CollectionsQueueRequest(
                COLLECTIONS_ASSIGNEE_FILTER_TENANT_CODE,
                "USD",
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));
    }

    @Test
    void listsCollectionsAssignmentHistoryNewestFirst() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "Collections History Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "Collections History Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "Collections History Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "arc-pay-1033",
            "so-pay-1033",
            "inv-pay-1033",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "inv-pay-1033",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant reassignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400 + 60);
        testClock.setInstant(reassignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
            "inv-pay-1033",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
                "inv-pay-1033",
                0,
                1
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-COLL-ASSIGN-HISTORY"))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1033"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedBy").value("manager@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedAt").value(reassignedAt.toString()));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_TENANT_CODE,
                "inv-pay-1033",
                1,
                1
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedBy").value("manager@arcanaerp.com"));
    }

    @Test
    void filtersCollectionsAssignmentHistoryByAssigneeAndAssignedAtRange() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "Collections History Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "Collections History Tenant",
            "COLLECTOR",
            "Collector",
            "collector-c@arcanaerp.com",
            "Collector C"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "Collections History Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "arc-pay-1035",
            "so-pay-1035",
            "inv-pay-1035",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        Instant firstAssignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(firstAssignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "inv-pay-1035",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant secondAssignedAt = firstAssignedAt.plusSeconds(60);
        testClock.setInstant(secondAssignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
            "inv-pay-1035",
            "collector-c@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
                "inv-pay-1035",
                0,
                10,
                "assignedTo",
                "collector-c@arcanaerp.com",
                "assignedAtFrom",
                secondAssignedAt.minusSeconds(1).toString(),
                "assignedAtTo",
                secondAssignedAt.toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1035"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-c@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedAt").value(secondAssignedAt.toString()));
    }

    @Test
    void rejectsInvalidCollectionsAssignmentHistoryFilters() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
                "inv-pay-1033",
                0,
                10,
                "assignedTo",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
                "inv-pay-1033",
                0,
                10,
                "assignedAtFrom",
                "not-an-instant"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.collectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_HISTORY_FILTER_TENANT_CODE,
                "inv-pay-1033",
                0,
                10,
                "assignedAtFrom",
                Instant.parse("2026-03-12T00:01:00Z").toString(),
                "assignedAtTo",
                Instant.parse("2026-03-12T00:00:00Z").toString()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedAtFrom must be before or equal to assignedAtTo"));
    }

    @Test
    void listsTenantCollectionsAssignmentHistoryWithFilters() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "Collections Feed Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "Collections Feed Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "Collections Feed Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "arc-pay-1036",
            "so-pay-1036",
            "inv-pay-1036",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "arc-pay-1037",
            "so-pay-1037",
            "inv-pay-1037",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );

        Instant firstAssignedAt = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(firstAssignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "inv-pay-1036",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant secondAssignedAt = firstAssignedAt.plusSeconds(60);
        testClock.setInstant(secondAssignedAt);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
            "inv-pay-1037",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-b@arcanaerp.com",
                "assignedAtFrom",
                secondAssignedAt.minusSeconds(1).toString(),
                "assignedAtTo",
                secondAssignedAt.toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value("TENANT-COLL-ASSIGN-FEED"))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1037"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedAt").value(secondAssignedAt.toString()));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
                0,
                10,
                "invoiceNumber",
                "inv-pay-1036"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].invoiceNumber").value("INV-PAY-1036"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-a@arcanaerp.com"));
    }

    @Test
    void rejectsInvalidTenantCollectionsAssignmentHistoryFilters() throws Exception {
        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
                0,
                10,
                "invoiceNumber",
                "   "
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invoiceNumber query parameter must not be blank"));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsAssignmentHistoryRequest(
                COLLECTIONS_ASSIGNMENT_TENANT_HISTORY_TENANT_CODE,
                0,
                10,
                "assignedAtFrom",
                "not-an-instant"
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedAtFrom query parameter must be a valid ISO-8601 instant"));
    }

    @Test
    void listsTenantCollectionsAssignmentSummaryByAssignee() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "Collections Summary Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "Collections Summary Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "Collections Summary Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "arc-pay-1038",
            "so-pay-1038",
            "inv-pay-1038",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "arc-pay-1039",
            "so-pay-1039",
            "inv-pay-1039",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "arc-pay-1040",
            "so-pay-1040",
            "inv-pay-1040",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(20 * 86400)
        );

        testClock.setInstant(PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400));
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "inv-pay-1038",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
            "inv-pay-1039",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.tenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_SUMMARY_TENANT_CODE,
                "USD",
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].assignedTo").value("collector-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].assignedInvoiceCount").value(1))
            .andExpect(jsonPath("$.items[0].totalOutstandingAmount").value(10.0))
            .andExpect(jsonPath("$.items[1].assignedTo").value("collector-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].assignedInvoiceCount").value(1))
            .andExpect(jsonPath("$.items[2].assignedTo").doesNotExist())
            .andExpect(jsonPath("$.items[2].assignedInvoiceCount").value(1))
            .andExpect(jsonPath("$.items[2].totalOutstandingAmount").value(10.0));
    }

    @Test
    void listsDailyTenantCollectionsAssignmentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "Collections Daily Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "Collections Daily Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "Collections Daily Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "arc-pay-1041",
            "so-pay-1041",
            "inv-pay-1041",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "arc-pay-1042",
            "so-pay-1042",
            "inv-pay-1042",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "arc-pay-1043",
            "so-pay-1043",
            "inv-pay-1043",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(20 * 86400)
        );

        Instant firstDay = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(firstDay);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "inv-pay-1041",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "inv-pay-1042",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant secondDay = firstDay.plusSeconds(86400);
        testClock.setInstant(secondDay);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
            "inv-pay-1043",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.dailyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value(secondDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value(firstDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[1].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[1].invoiceCount").value(2));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.dailyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_DAILY_SUMMARY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-a@arcanaerp.com",
                "assignedAtFrom",
                firstDay.minusSeconds(1).toString(),
                "assignedAtTo",
                firstDay.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessDate").value(firstDay.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(2));
    }

    @Test
    void listsWeeklyTenantCollectionsAssignmentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "Collections Weekly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "Collections Weekly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "Collections Weekly Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "arc-pay-1044",
            "so-pay-1044",
            "inv-pay-1044",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "arc-pay-1045",
            "so-pay-1045",
            "inv-pay-1045",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "arc-pay-1046",
            "so-pay-1046",
            "inv-pay-1046",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(20 * 86400)
        );

        Instant firstWeek = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130 * 86400);
        testClock.setInstant(firstWeek);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "inv-pay-1044",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "inv-pay-1045",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant secondWeek = firstWeek.plusSeconds(7 * 86400);
        testClock.setInstant(secondWeek);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
            "inv-pay-1046",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.weeklyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value(secondWeek
                .atOffset(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toString()))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value(firstWeek
                .atOffset(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toString()))
            .andExpect(jsonPath("$.items[1].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[1].invoiceCount").value(2));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.weeklyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_WEEKLY_SUMMARY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-a@arcanaerp.com",
                "assignedAtFrom",
                firstWeek.minusSeconds(1).toString(),
                "assignedAtTo",
                firstWeek.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value(firstWeek
                .atOffset(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toString()))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(2));
    }

    @Test
    void listsMonthlyTenantCollectionsAssignmentSummary() throws Exception {
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "Collections Monthly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-a@arcanaerp.com",
            "Collector A"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "Collections Monthly Tenant",
            "COLLECTOR",
            "Collector",
            "collector-b@arcanaerp.com",
            "Collector B"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.createIdentityUser(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "Collections Monthly Tenant",
            "MANAGER",
            "Manager",
            "manager@arcanaerp.com",
            "Manager"
        ).andExpect(status().isCreated());
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "arc-pay-1047",
            "so-pay-1047",
            "inv-pay-1047",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(10 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "arc-pay-1048",
            "so-pay-1048",
            "inv-pay-1048",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(15 * 86400)
        );
        PaymentsWebIntegrationTestSupport.seedIssuedInvoice(
            mockMvc,
            testClock,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "arc-pay-1049",
            "so-pay-1049",
            "inv-pay-1049",
            PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(20 * 86400)
        );

        Instant firstMonth = PaymentsDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(130L * 86400);
        testClock.setInstant(firstMonth);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "inv-pay-1047",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "inv-pay-1048",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        Instant secondMonth = Instant.parse("2026-08-05T00:00:00Z");
        testClock.setInstant(secondMonth);
        PaymentsWebIntegrationTestSupport.assignOver90CollectionsInvoice(
            mockMvc,
            COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
            "inv-pay-1049",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com"
        ).andExpect(status().isOk());

        mockMvc.perform(PaymentsWebIntegrationTestSupport.monthlyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
                0,
                10
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-08"))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessMonth").value(firstMonth.atOffset(java.time.ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).toString().substring(0, 7)))
            .andExpect(jsonPath("$.items[1].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[1].invoiceCount").value(2));

        mockMvc.perform(PaymentsWebIntegrationTestSupport.monthlyTenantCollectionsAssignmentSummaryRequest(
                COLLECTIONS_ASSIGNMENT_MONTHLY_SUMMARY_TENANT_CODE,
                0,
                10,
                "assignedTo",
                "collector-a@arcanaerp.com",
                "assignedAtFrom",
                firstMonth.minusSeconds(1).toString(),
                "assignedAtTo",
                firstMonth.plusSeconds(1).toString()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value(firstMonth.atOffset(java.time.ZoneOffset.UTC).toLocalDate().withDayOfMonth(1).toString().substring(0, 7)))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(2));
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
