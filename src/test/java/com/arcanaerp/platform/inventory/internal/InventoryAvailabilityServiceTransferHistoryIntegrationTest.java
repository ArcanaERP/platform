package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceTransferHistoryIntegrationTest {

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
    void listsTransferHistoryNewestFirstWithPaginationMetadata() throws Exception {
        String sku = "ARC-SVC-TR-1";
        seedTransferItems(sku);

        inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "main",
                "wh-west",
                new BigDecimal("4"),
                "Transfer One",
                "ops-a@arcanaerp.com",
                "order",
                "SO-SVC-1"
            )
        );
        Thread.sleep(25);
        inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "wh-west",
                "wh-east",
                new BigDecimal("2"),
                "Transfer Two",
                "ops-b@arcanaerp.com",
                "order",
                "SO-SVC-2"
            )
        );
        Thread.sleep(25);
        inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "wh-east",
                "main",
                new BigDecimal("1"),
                "Transfer Three",
                "ops-a@arcanaerp.com",
                "fulfillment",
                "FUL-SVC-3"
            )
        );

        var firstPage = inventoryAvailability.listTransfers(
            sku,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new PageQuery(0, 2)
        );

        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalItems()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.items()).extracting(transfer -> transfer.reason()).containsExactly("Transfer Three", "Transfer Two");

        var secondPage = inventoryAvailability.listTransfers(
            sku,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new PageQuery(1, 2)
        );

        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(secondPage.totalItems()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();
        assertThat(secondPage.items()).extracting(transfer -> transfer.reason()).containsExactly("Transfer One");
    }

    @Test
    void filtersTransferHistoryByActorLocationAndReference() throws Exception {
        String sku = "ARC-SVC-TR-2";
        seedTransferItems(sku);

        inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "main",
                "wh-west",
                new BigDecimal("3"),
                "Transfer Alpha",
                "ops-a@arcanaerp.com",
                "order",
                "SO-SVC-A"
            )
        );
        Thread.sleep(25);
        inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "wh-west",
                "wh-east",
                new BigDecimal("2"),
                "Transfer Beta",
                "ops-b@arcanaerp.com",
                "order",
                "SO-SVC-B"
            )
        );

        var filtered = inventoryAvailability.listTransfers(
            sku,
            "wh-west",
            "wh-east",
            "ops-b@arcanaerp.com",
            "order",
            "SO-SVC-B",
            null,
            null,
            new PageQuery(0, 10)
        );

        assertThat(filtered.totalItems()).isEqualTo(1);
        assertThat(filtered.items()).hasSize(1);
        assertThat(filtered.items().getFirst().reason()).isEqualTo("Transfer Beta");
        assertThat(filtered.items().getFirst().sourceLocationCode()).isEqualTo("WH-WEST");
        assertThat(filtered.items().getFirst().destinationLocationCode()).isEqualTo("WH-EAST");
        assertThat(filtered.items().getFirst().adjustedBy()).isEqualTo("ops-b@arcanaerp.com");
        assertThat(filtered.items().getFirst().referenceType()).isEqualTo("ORDER");
        assertThat(filtered.items().getFirst().referenceId()).isEqualTo("SO-SVC-B");
    }

    @Test
    void throwsForUnknownSkuTransferHistory() {
        assertThatThrownBy(() -> inventoryAvailability.listTransfers(
                "missing-sku",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PageQuery(0, 10)
            ))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Inventory item not found for SKU: MISSING-SKU");
    }

    @Test
    void rejectsTransferHistoryWhenPageIsNegative() {
        assertThatThrownBy(() -> inventoryAvailability.listTransfers(
                "arc-svc-tr-3",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageQuery.of(-1, 10)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("page must be greater than or equal to zero");
    }

    @Test
    void rejectsTransferHistoryWhenSizeOutsideBounds() {
        assertThatThrownBy(() -> inventoryAvailability.listTransfers(
                "arc-svc-tr-4",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageQuery.of(0, 0)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");

        assertThatThrownBy(() -> inventoryAvailability.listTransfers(
                "arc-svc-tr-4",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageQuery.of(0, 101)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");
    }

    private void seedTransferItems(String sku) {
        Instant seededAt = Instant.parse("2026-03-04T00:00:00Z");
        inventoryItemRepository.save(InventoryItem.create(sku, "main", new BigDecimal("20"), seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-west", new BigDecimal("5"), seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-east", new BigDecimal("2"), seededAt));
    }
}
