package com.arcanaerp.platform.orders.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderDomainTest {

    @Test
    void salesOrderCreateNormalizesOrderFields() {
        SalesOrder order = SalesOrder.create(
            "  so-1000 ",
            " Customer@Acme.COM ",
            " usd ",
            new BigDecimal("10.00"),
            Instant.parse("2026-02-28T00:00:00Z")
        );

        assertThat(order.getOrderNumber()).isEqualTo("SO-1000");
        assertThat(order.getCustomerEmail()).isEqualTo("customer@acme.com");
        assertThat(order.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void salesOrderCreateRejectsNonPositiveTotalAmount() {
        assertThatThrownBy(() ->
            SalesOrder.create(
                "SO-1001",
                "customer@acme.com",
                "USD",
                BigDecimal.ZERO,
                Instant.parse("2026-02-28T00:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("totalAmount must be greater than zero");
    }

    @Test
    void salesOrderLineCreateCalculatesLineTotal() {
        SalesOrderLine line = SalesOrderLine.create(
            UUID.randomUUID(),
            1,
            "arc-1000",
            new BigDecimal("2.5"),
            new BigDecimal("4.00"),
            Instant.parse("2026-02-28T00:00:00Z")
        );

        assertThat(line.getProductSku()).isEqualTo("ARC-1000");
        assertThat(line.getLineTotal()).isEqualByComparingTo("10.0000");
    }
}
