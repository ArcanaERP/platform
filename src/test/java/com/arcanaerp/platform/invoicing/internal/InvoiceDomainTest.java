package com.arcanaerp.platform.invoicing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InvoiceDomainTest {

    @Test
    void invoiceCreateNormalizesCoreFields() {
        Invoice invoice = Invoice.create(
            " tenant-01 ",
            " inv-1000 ",
            " so-1000 ",
            " usd ",
            new BigDecimal("10.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z")
        );

        assertThat(invoice.getTenantCode()).isEqualTo("TENANT-01");
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-1000");
        assertThat(invoice.getOrderNumber()).isEqualTo("SO-1000");
        assertThat(invoice.getCurrencyCode()).isEqualTo("USD");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getIssuedAt()).isNull();
        assertThat(invoice.getVoidedAt()).isNull();
    }

    @Test
    void invoiceCreateRejectsDueDateBeforeCreatedAt() {
        assertThatThrownBy(() -> Invoice.create(
            "TENANT-01",
            "INV-1001",
            "SO-1001",
            "USD",
            new BigDecimal("10.00"),
            Instant.parse("2026-03-10T00:00:00Z"),
            Instant.parse("2026-03-09T23:59:59Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("dueAt must be on or after createdAt");
    }

    @Test
    void invoiceTransitionsFromDraftToIssuedThenVoid() {
        Invoice invoice = Invoice.create(
            "TENANT-01",
            "INV-1002",
            "SO-1002",
            "USD",
            new BigDecimal("10.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z")
        );
        Instant issuedAt = Instant.parse("2026-03-02T00:00:00Z");
        Instant voidedAt = Instant.parse("2026-03-03T00:00:00Z");

        invoice.transitionTo(InvoiceStatus.ISSUED, issuedAt);
        invoice.transitionTo(InvoiceStatus.VOID, voidedAt);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VOID);
        assertThat(invoice.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(invoice.getVoidedAt()).isEqualTo(voidedAt);
    }

    @Test
    void invoiceRejectsTransitionOutOfVoid() {
        Invoice invoice = Invoice.create(
            "TENANT-01",
            "INV-1003",
            "SO-1003",
            "USD",
            new BigDecimal("10.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z")
        );
        invoice.transitionTo(InvoiceStatus.VOID, Instant.parse("2026-03-02T00:00:00Z"));

        assertThatThrownBy(() -> invoice.transitionTo(InvoiceStatus.ISSUED, Instant.parse("2026-03-03T00:00:00Z")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invoice status transition not allowed: VOID -> ISSUED");
    }
}
