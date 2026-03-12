package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentDomainTest {

    @Test
    void paymentCreateNormalizesCoreFields() {
        Payment payment = Payment.create(
            " tenant-01 ",
            " pay-1000 ",
            " inv-1000 ",
            new BigDecimal("12.50"),
            " usd ",
            Instant.parse("2026-03-12T00:00:00Z"),
            Instant.parse("2026-03-12T00:01:00Z")
        );

        assertThat(payment.getTenantCode()).isEqualTo("TENANT-01");
        assertThat(payment.getPaymentReference()).isEqualTo("PAY-1000");
        assertThat(payment.getInvoiceNumber()).isEqualTo("INV-1000");
        assertThat(payment.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void paymentCreateRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> Payment.create(
            "TENANT-01",
            "PAY-1001",
            "INV-1001",
            BigDecimal.ZERO,
            "USD",
            Instant.parse("2026-03-12T00:00:00Z"),
            Instant.parse("2026-03-12T00:01:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("amount must be greater than zero");
    }
}
