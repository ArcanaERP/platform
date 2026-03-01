package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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
                "main",
                new BigDecimal("20"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                item.getLocationCode(),
                null,
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
                item.getLocationCode(),
                null,
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

    @Test
    void filtersHistoryByAdjustedByAndDateRange() {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9301",
                "main",
                new BigDecimal("20"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                item.getLocationCode(),
                null,
                new BigDecimal("20"),
                new BigDecimal("-2"),
                new BigDecimal("18"),
                "Cycle count correction",
                "ops-a@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                item.getLocationCode(),
                null,
                new BigDecimal("18"),
                new BigDecimal("4"),
                new BigDecimal("22"),
                "Receiving posted",
                "ops-b@arcanaerp.com",
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var filteredByActor = inventoryAdjustmentRepository.findHistoryFiltered(
            item.getId(),
            "ops-b@arcanaerp.com",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var filteredByRange = inventoryAdjustmentRepository.findHistoryFiltered(
            item.getId(),
            null,
            Instant.parse("2026-03-01T01:30:00Z"),
            Instant.parse("2026-03-01T02:30:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(filteredByActor.getTotalElements()).isEqualTo(1);
        assertThat(filteredByActor.getContent().getFirst().getReason()).isEqualTo("Receiving posted");
        assertThat(filteredByRange.getTotalElements()).isEqualTo(1);
        assertThat(filteredByRange.getContent().getFirst().getReason()).isEqualTo("Receiving posted");
    }

    @Test
    void findsTransferAdjustmentsByTransferIdOldestFirst() {
        InventoryItem sourceItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9302",
                "main",
                new BigDecimal("20"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        InventoryItem destinationItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9302",
                "wh-east",
                new BigDecimal("2"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        var transferId = java.util.UUID.randomUUID();
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                sourceItem.getId(),
                sourceItem.getSku(),
                sourceItem.getLocationCode(),
                transferId,
                new BigDecimal("20"),
                new BigDecimal("-4"),
                new BigDecimal("16"),
                "Inter-warehouse transfer",
                "ops-a@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                destinationItem.getId(),
                destinationItem.getSku(),
                destinationItem.getLocationCode(),
                transferId,
                new BigDecimal("2"),
                new BigDecimal("4"),
                new BigDecimal("6"),
                "Inter-warehouse transfer",
                "ops-a@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:01Z")
            )
        );

        List<InventoryAdjustment> transferAdjustments = inventoryAdjustmentRepository.findByTransferIdOrderByAdjustedAtAsc(transferId);

        assertThat(transferAdjustments).hasSize(2);
        assertThat(transferAdjustments.getFirst().getLocationCode()).isEqualTo("MAIN");
        assertThat(transferAdjustments.getFirst().getQuantityDelta()).isEqualByComparingTo("-4");
        assertThat(transferAdjustments.get(1).getLocationCode()).isEqualTo("WH-EAST");
        assertThat(transferAdjustments.get(1).getQuantityDelta()).isEqualByComparingTo("4");
    }
}
