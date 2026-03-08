package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalHistoryIntegrationTest {

    private static final String REVERSAL_REASON = InventoryReversalTestConstants.REVERSAL_REASON;
    private static final String REVERSAL_ACTOR = InventoryReversalTestConstants.REVERSAL_ACTOR;

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
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("3"),
            "SO-RV-1"
        );

        InventoryTransferReversalServiceTestFixture.reverseTransfer(
            inventoryAvailability,
            originalTransfer.transferId(),
            REVERSAL_REASON,
            REVERSAL_ACTOR,
            null
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
        assertThat(reversal.reason()).isEqualTo(REVERSAL_REASON);
        assertThat(reversal.adjustedBy()).isEqualTo(REVERSAL_ACTOR);
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
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("2"),
            "SO-RV-2"
        );

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(0);
        assertThat(reversals.items()).isEmpty();
    }

    @Test
    void usesDefaultPaginationWhenReversalHistoryPageQueryValuesAreOmitted() {
        String sku = "ARC-SVC-RV-2A";
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("2"),
            "SO-RV-2A"
        );

        InventoryTransferReversalServiceTestFixture.reverseTransfer(
            inventoryAvailability,
            originalTransfer.transferId(),
            REVERSAL_REASON,
            REVERSAL_ACTOR,
            null
        );

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), PageQuery.of(null, null));
        assertThat(reversals.page()).isEqualTo(PageQuery.DEFAULT_PAGE);
        assertThat(reversals.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.totalPages()).isEqualTo(1);
        assertThat(reversals.hasNext()).isFalse();
        assertThat(reversals.hasPrevious()).isFalse();
        assertThat(reversals.items()).hasSize(1);
        assertThat(reversals.items().getFirst().referenceId()).isEqualTo(originalTransfer.transferId().toString());
    }

    @Test
    void throwsWhenListingReversalHistoryForUnknownTransferId() {
        UUID unknownTransferId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        assertThatThrownBy(() -> inventoryAvailability.listReversals(unknownTransferId, new PageQuery(0, 10)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Inventory transfer not found: " + unknownTransferId);
    }

    @Test
    void rejectsReversalHistoryWhenPageIsNegative() {
        assertThatThrownBy(() -> inventoryAvailability.listReversals(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                PageQuery.of(-1, 10)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("page must be greater than or equal to zero");
    }

    @Test
    void rejectsReversalHistoryWhenSizeOutsideBounds() {
        UUID transferId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        assertThatThrownBy(() -> inventoryAvailability.listReversals(transferId, PageQuery.of(0, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");

        assertThatThrownBy(() -> inventoryAvailability.listReversals(transferId, PageQuery.of(0, 101)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");
    }

}
