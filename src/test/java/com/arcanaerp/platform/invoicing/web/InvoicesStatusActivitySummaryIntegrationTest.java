package com.arcanaerp.platform.invoicing.web;

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
@Import(InvoicesDeterministicClockTestSupport.Configuration.class)
class InvoicesStatusActivitySummaryIntegrationTest {

    private static final Instant DUE_AT = Instant.parse("2026-04-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoicesDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummariesAtWebBoundary() throws Exception {
        seedInvoiceStatusTransition(
            "tenant-inv-act",
            "arc-invact-001",
            "so-invact-001",
            "inv-invact-001",
            "ISSUED",
            Instant.parse("2026-04-22T10:00:00Z"),
            "Ready to bill",
            "agent01@invoices.com"
        );
        seedInvoiceStatusTransition(
            "tenant-inv-act",
            "arc-invact-002",
            "so-invact-002",
            "inv-invact-002",
            "VOID",
            Instant.parse("2026-04-23T11:00:00Z"),
            "Customer request",
            "agent02@invoices.com"
        );
        seedIssuedThenVoidedInvoice(
            "tenant-inv-act",
            "arc-invact-003",
            "so-invact-003",
            "inv-invact-003",
            Instant.parse("2026-05-04T12:00:00Z"),
            Instant.parse("2026-05-05T12:00:00Z")
        );
        seedInvoiceStatusTransition(
            "tenant-other-act",
            "arc-invact-004",
            "so-invact-004",
            "inv-invact-004",
            "ISSUED",
            Instant.parse("2026-05-06T12:00:00Z"),
            "Other tenant",
            "agent01@invoices.com"
        );

        mockMvc.perform(
            InvoicesWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                0,
                10,
                "tenantCode", "tenant-inv-act",
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(4))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-05-05"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(1))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-05-04"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2026-04-23"))
            .andExpect(jsonPath("$.items[3].businessDate").value("2026-04-22"));

        mockMvc.perform(
            InvoicesWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
                0,
                10,
                "tenantCode", "tenant-inv-act",
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(2))
            .andExpect(jsonPath("$.items[0].invoiceCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[1].transitionCount").value(2))
            .andExpect(jsonPath("$.items[1].invoiceCount").value(2));

        mockMvc.perform(
            InvoicesWebIntegrationTestSupport.monthlyStatusActivitySummaryRequest(
                0,
                10,
                "tenantCode", "tenant-inv-act",
                "currentStatus", "ISSUED",
                "changedBy", "AGENT01@INVOICES.COM",
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-05"))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-04"));
    }

    @Test
    void paginatesStatusActivitySummaryBuckets() throws Exception {
        seedInvoiceStatusTransition(
            "tenant-inv-act-page",
            "arc-invact-pg-001",
            "so-invact-pg-001",
            "inv-invact-pg-001",
            "ISSUED",
            Instant.parse("2026-06-22T10:00:00Z"),
            "Ready to bill",
            "agent01@invoices.com"
        );
        seedInvoiceStatusTransition(
            "tenant-inv-act-page",
            "arc-invact-pg-002",
            "so-invact-pg-002",
            "inv-invact-pg-002",
            "VOID",
            Instant.parse("2026-07-04T12:00:00Z"),
            "Customer request",
            "agent02@invoices.com"
        );

        mockMvc.perform(
            InvoicesWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                0,
                1,
                "tenantCode", "tenant-inv-act-page",
                "changedAtFrom", "2026-06-01T00:00:00Z",
                "changedAtTo", "2026-07-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-07-04"));

        mockMvc.perform(
            InvoicesWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                1,
                1,
                "tenantCode", "tenant-inv-act-page",
                "changedAtFrom", "2026-06-01T00:00:00Z",
                "changedAtTo", "2026-07-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-06-22"));
    }

    private void seedIssuedThenVoidedInvoice(
        String tenantCode,
        String sku,
        String orderNumber,
        String invoiceNumber,
        Instant issuedAt,
        Instant voidedAt
    ) throws Exception {
        seedInvoiceStatusTransition(
            tenantCode,
            sku,
            orderNumber,
            invoiceNumber,
            "ISSUED",
            issuedAt,
            "Ready to bill",
            "agent01@invoices.com"
        );
        testClock.setInstant(voidedAt);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(
            mockMvc,
            invoiceNumber,
            "VOID",
            "Customer dispute",
            "agent02@invoices.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VOID"));
    }

    private void seedInvoiceStatusTransition(
        String tenantCode,
        String sku,
        String orderNumber,
        String invoiceNumber,
        String targetStatus,
        Instant changedAt,
        String reason,
        String changedBy
    ) throws Exception {
        InvoicesWebIntegrationTestSupport.registerProduct(mockMvc, sku)
            .andExpect(status().isCreated());
        InvoicesWebIntegrationTestSupport.createConfirmedSingleLineOrder(mockMvc, testClock, orderNumber, sku)
            .andExpect(status().isOk());
        testClock.resetToBaseInstant();
        InvoicesWebIntegrationTestSupport.createInvoice(mockMvc, tenantCode, invoiceNumber, orderNumber, DUE_AT)
            .andExpect(status().isCreated());

        testClock.setInstant(changedAt);
        InvoicesWebIntegrationTestSupport.transitionInvoiceStatus(mockMvc, invoiceNumber, targetStatus, reason, changedBy)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(targetStatus));
    }
}
