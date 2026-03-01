package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void findsInventoryItemBySku() {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9100",
                new BigDecimal("42"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        InventoryItem item = inventoryItemRepository.findBySku("ARC-9100").orElseThrow();

        assertThat(item.getSku()).isEqualTo("ARC-9100");
        assertThat(item.getOnHandQuantity()).isEqualByComparingTo("42");
    }
}
