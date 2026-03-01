package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryItemDomainTest {

    @Test
    void createNormalizesSku() {
        InventoryItem item = InventoryItem.create(
            "  arc-9000  ",
            new BigDecimal("25"),
            Instant.parse("2026-03-01T00:00:00Z")
        );

        assertThat(item.getSku()).isEqualTo("ARC-9000");
    }

    @Test
    void createRejectsNegativeOnHandQuantity() {
        assertThatThrownBy(() ->
            InventoryItem.create(
                "ARC-9001",
                new BigDecimal("-1"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("onHandQuantity must be zero or greater");
    }
}
