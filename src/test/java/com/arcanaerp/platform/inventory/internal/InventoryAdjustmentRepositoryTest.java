package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InventoryAdjustmentRepositoryTest {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Test
    void findsAdjustmentsByInventoryItemNewestFirst() {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9300",
                new BigDecimal("20"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                new BigDecimal("20"),
                new BigDecimal("-3"),
                new BigDecimal("17"),
                "Cycle count correction",
                "ops-a@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                new BigDecimal("17"),
                new BigDecimal("5"),
                new BigDecimal("22"),
                "Receiving posted",
                "ops-b@arcanaerp.com",
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());

        assertThat(adjustments).hasSize(2);
        assertThat(adjustments.getFirst().getReason()).isEqualTo("Receiving posted");
        assertThat(adjustments.getFirst().getQuantityDelta()).isEqualByComparingTo("5");
        assertThat(adjustments.get(1).getReason()).isEqualTo("Cycle count correction");
        assertThat(adjustments.get(1).getQuantityDelta()).isEqualByComparingTo("-3");
    }
}
