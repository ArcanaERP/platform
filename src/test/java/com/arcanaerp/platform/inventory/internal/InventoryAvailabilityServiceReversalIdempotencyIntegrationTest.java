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
import java.util.UUID;
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
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        assertThat(replayedReversal.transferId()).isEqualTo(firstReversal.transferId());
        assertThat(replayedReversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayedReversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(firstReversal.transferId());
    }

    @Test
    void replaysExistingReversalWhenIdempotencyKeyOnlyDiffersBySurroundingWhitespace() {
        String sku = "ARC-SVC-IDEMP-7";
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
                "SO-IDEMP-7"
            )
        );

        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", " reverse-svc-replay-7 ")
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", "reverse-svc-replay-7")
        );

        assertThat(replayedReversal.transferId()).isEqualTo(firstReversal.transferId());
        assertThat(replayedReversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayedReversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(firstReversal.transferId());
    }

    @Test
    void rejectsReversalReplayWhenPayloadDiffersForTrimEquivalentIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-8";
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
                "SO-IDEMP-8"
            )
        );

        inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", " reverse-svc-replay-8 ")
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "Different reason", "reverse-svc-replay-8")
            ))
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: "
                + originalTransfer.transferId());
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
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "Different reason", idempotencyKey)
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
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "reversal posted", idempotencyKey)
            ))
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: "
                + originalTransfer.transferId());
    }

    @Test
    void replaysReversalWhenReasonOnlyDiffersByTrailingWhitespaceForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-6";
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
                "SO-IDEMP-6"
            )
        );

        String idempotencyKey = "reverse-svc-replay-6";
        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted ", idempotencyKey)
        );

        assertThat(replayedReversal.transferId()).isEqualTo(firstReversal.transferId());
        assertThat(replayedReversal.reason()).isEqualTo("Reversal posted");

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(firstReversal.transferId());
        assertThat(reversals.items().getFirst().reason()).isEqualTo("Reversal posted");
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
            reverseCommand(originalTransfer.transferId(), "Reversal posted", idempotencyKey)
        );

        assertThatThrownBy(() -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "Reversal posted", "warehouse@arcanaerp.com", idempotencyKey)
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
            reverseCommand(originalTransfer.transferId(), "Reversal posted", "Ops@ArcanaERP.com", idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), "Reversal posted", "OPS@ARCANAERP.COM", idempotencyKey)
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
        InventoryIdempotencyTestFixture.seedTransferItems(
            inventoryItemRepository,
            sku,
            new BigDecimal("20"),
            new BigDecimal("5"),
            Instant.parse("2026-03-04T00:00:00Z")
        );
    }

    private static ReverseInventoryTransferCommand reverseCommand(
        UUID transferId,
        String reason,
        String idempotencyKey
    ) {
        return reverseCommand(transferId, reason, "ops@arcanaerp.com", idempotencyKey);
    }

    private static ReverseInventoryTransferCommand reverseCommand(
        UUID transferId,
        String reason,
        String adjustedBy,
        String idempotencyKey
    ) {
        return new ReverseInventoryTransferCommand(transferId, reason, adjustedBy, idempotencyKey);
    }
}
