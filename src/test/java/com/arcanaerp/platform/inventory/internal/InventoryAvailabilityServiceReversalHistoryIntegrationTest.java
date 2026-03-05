package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalHistoryIntegrationTest {

    @Autowired
    private InventoryAvailability inventoryAvailability;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryTables() {
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
    }

    @Test
    void listsReversalsWithPaginationAndOriginalTransferLinkSemantics() {
        String sku = "ARC-SVC-RV-1";
        seedTransferItems(sku);

        var originalTransfer = inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "main",
                "wh-east",
                new BigDecimal("3"),
                "Original transfer",
                "ops@arcanaerp.com",
                "order",
                "SO-RV-1"
            )
        );

        inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                null
            )
        );

        var firstPage = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 1));

        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.size()).isEqualTo(1);
        assertThat(firstPage.totalItems()).isEqualTo(1);
        assertThat(firstPage.totalPages()).isEqualTo(1);
        assertThat(firstPage.hasNext()).isFalse();
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.items()).hasSize(1);

        var reversal = firstPage.items().getFirst();
        assertThat(reversal.sku()).isEqualTo("ARC-SVC-RV-1");
        assertThat(reversal.sourceLocationCode()).isEqualTo("WH-EAST");
        assertThat(reversal.destinationLocationCode()).isEqualTo("MAIN");
        assertThat(reversal.quantity()).isEqualByComparingTo("3");
        assertThat(reversal.reason()).isEqualTo("Reversal posted");
        assertThat(reversal.adjustedBy()).isEqualTo("ops@arcanaerp.com");
        assertThat(reversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(reversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var secondPage = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(1, 1));
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(1);
        assertThat(secondPage.totalItems()).isEqualTo(1);
        assertThat(secondPage.totalPages()).isEqualTo(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();
        assertThat(secondPage.items()).isEmpty();
    }

    @Test
    void returnsEmptyReversalHistoryWhenNoReversalExists() {
        String sku = "ARC-SVC-RV-2";
        seedTransferItems(sku);

        var originalTransfer = inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "main",
                "wh-east",
                new BigDecimal("2"),
                "Original transfer",
                "ops@arcanaerp.com",
                "order",
                "SO-RV-2"
            )
        );

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(0);
        assertThat(reversals.items()).isEmpty();
    }

    @Test
    void throwsWhenListingReversalHistoryForUnknownTransferId() {
        UUID unknownTransferId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        assertThatThrownBy(() -> inventoryAvailability.listReversals(unknownTransferId, new PageQuery(0, 10)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Inventory transfer not found: " + unknownTransferId);
    }

    private void seedTransferItems(String sku) {
        Instant seededAt = Instant.parse("2026-03-04T00:00:00Z");
        inventoryItemRepository.save(InventoryItem.create(sku, "main", new BigDecimal("20"), seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-east", new BigDecimal("5"), seededAt));
    }
}
