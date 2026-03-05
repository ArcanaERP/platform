package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.ReversalIdempotencyPayloadConflictException;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalIdempotencyIntegrationTest {

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
    void replaysExistingReversalForSameIdempotencyKeyAndPayload() {
        String sku = "ARC-SVC-IDEMP-1";
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
                "SO-IDEMP-1"
            )
        );

        String idempotencyKey = "reverse-svc-replay-1";
        var firstReversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
        );

        assertThat(replayedReversal.transferId()).isEqualTo(firstReversal.transferId());
        assertThat(replayedReversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayedReversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(firstReversal.transferId());
    }

    @Test
    void rejectsReversalReplayWhenPayloadDiffersForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-2";
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
                "SO-IDEMP-2"
            )
        );

        String idempotencyKey = "reverse-svc-replay-2";
        inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                new ReverseInventoryTransferCommand(
                    originalTransfer.transferId(),
                    "Different reason",
                    "ops@arcanaerp.com",
                    idempotencyKey
                )
            ))
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: "
                + originalTransfer.transferId());
    }

    @Test
    void rejectsReversalReplayWhenReasonOnlyDiffersByCaseForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-5";
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
                "SO-IDEMP-5"
            )
        );

        String idempotencyKey = "reverse-svc-replay-5";
        inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                new ReverseInventoryTransferCommand(
                    originalTransfer.transferId(),
                    "reversal posted",
                    "ops@arcanaerp.com",
                    idempotencyKey
                )
            ))
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: "
                + originalTransfer.transferId());
    }

    @Test
    void rejectsReversalReplayWhenAdjustedByDiffersForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-4";
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
                "SO-IDEMP-4"
            )
        );

        String idempotencyKey = "reverse-svc-replay-4";
        inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                new ReverseInventoryTransferCommand(
                    originalTransfer.transferId(),
                    "Reversal posted",
                    "warehouse@arcanaerp.com",
                    idempotencyKey
                )
            ))
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: "
                + originalTransfer.transferId());
    }

    @Test
    void replaysForSamePayloadWhenAdjustedByOnlyDiffersByCase() {
        String sku = "ARC-SVC-IDEMP-3";
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
                "SO-IDEMP-3"
            )
        );

        String idempotencyKey = "reverse-svc-replay-3";
        var firstReversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "Ops@ArcanaERP.com",
                idempotencyKey
            )
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "OPS@ARCANAERP.COM",
                idempotencyKey
            )
        );

        assertThat(replayedReversal.transferId()).isEqualTo(firstReversal.transferId());
        assertThat(replayedReversal.adjustedBy()).isEqualTo("ops@arcanaerp.com");
        assertThat(replayedReversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayedReversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(firstReversal.transferId());
        assertThat(reversals.items().getFirst().adjustedBy()).isEqualTo("ops@arcanaerp.com");
    }

    private void seedTransferItems(String sku) {
        Instant seededAt = Instant.parse("2026-03-04T00:00:00Z");
        inventoryItemRepository.save(InventoryItem.create(sku, "main", new BigDecimal("20"), seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-east", new BigDecimal("5"), seededAt));
    }
}
