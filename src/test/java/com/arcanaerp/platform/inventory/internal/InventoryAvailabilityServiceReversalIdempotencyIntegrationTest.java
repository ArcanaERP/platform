package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryTransferView;
import com.arcanaerp.platform.inventory.ReversalIdempotencyPayloadConflictException;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalIdempotencyIntegrationTest {

    private static final String DEFAULT_REASON = InventoryReversalTestConstants.REVERSAL_REASON;
    private static final String DEFAULT_ACTOR = InventoryReversalTestConstants.REVERSAL_ACTOR;

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
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("3"), "SO-IDEMP-1");

        String idempotencyKey = "reverse-svc-replay-1";
        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        assertReplayInvariants(replayedReversal, firstReversal.transferId(), originalTransfer.transferId());

        assertSingleReversalHistoryContains(originalTransfer.transferId(), firstReversal.transferId());
    }

    @Test
    void replaysExistingReversalWhenIdempotencyKeyOnlyDiffersBySurroundingWhitespace() {
        String sku = "ARC-SVC-IDEMP-7";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("3"), "SO-IDEMP-7");

        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, " reverse-svc-replay-7 ")
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "reverse-svc-replay-7")
        );

        assertReplayInvariants(replayedReversal, firstReversal.transferId(), originalTransfer.transferId());

        assertSingleReversalHistoryContains(originalTransfer.transferId(), firstReversal.transferId());
    }

    @Test
    void rejectsReversalReplayWhenPayloadDiffersForTrimEquivalentIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-8";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-8");

        inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, " reverse-svc-replay-8 ")
        );

        assertPayloadConflict(
            originalTransfer.transferId(),
            () -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "Different reason", "reverse-svc-replay-8")
            )
        );
    }

    @Test
    void rejectsReversalReplayWhenPayloadDiffersForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-2";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-2");

        String idempotencyKey = "reverse-svc-replay-2";
        inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        assertPayloadConflict(
            originalTransfer.transferId(),
            () -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "Different reason", idempotencyKey)
            )
        );
    }

    @Test
    void rejectsReversalReplayWhenReasonOnlyDiffersByCaseForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-5";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-5");

        String idempotencyKey = "reverse-svc-replay-5";
        inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        assertPayloadConflict(
            originalTransfer.transferId(),
            () -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), "reversal posted", idempotencyKey)
            )
        );
    }

    @Test
    void replaysReversalWhenReasonOnlyDiffersByTrailingWhitespaceForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-6";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-6");

        String idempotencyKey = "reverse-svc-replay-6";
        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON + " ", idempotencyKey)
        );

        assertReplayInvariants(replayedReversal, firstReversal.transferId(), originalTransfer.transferId());
        assertThat(replayedReversal.reason()).isEqualTo(DEFAULT_REASON);

        var reversalHistory = assertSingleReversalHistoryContains(originalTransfer.transferId(), firstReversal.transferId());
        assertThat(reversalHistory.reason()).isEqualTo(DEFAULT_REASON);
    }

    @Test
    void rejectsReversalReplayWhenAdjustedByDiffersForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-4";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-4");

        String idempotencyKey = "reverse-svc-replay-4";
        inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, idempotencyKey)
        );

        assertPayloadConflict(
            originalTransfer.transferId(),
            () -> inventoryAvailability.reverseTransfer(
                reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "warehouse@arcanaerp.com", idempotencyKey)
            )
        );
    }

    @Test
    void replaysForSamePayloadWhenAdjustedByOnlyDiffersByCase() {
        String sku = "ARC-SVC-IDEMP-3";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-3");

        String idempotencyKey = "reverse-svc-replay-3";
        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "Ops@ArcanaERP.com", idempotencyKey)
        );

        var replayedReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "OPS@ARCANAERP.COM", idempotencyKey)
        );

        assertReplayInvariants(replayedReversal, firstReversal.transferId(), originalTransfer.transferId());
        assertThat(replayedReversal.adjustedBy()).isEqualTo(DEFAULT_ACTOR);

        var reversalHistory = assertSingleReversalHistoryContains(originalTransfer.transferId(), firstReversal.transferId());
        assertThat(reversalHistory.adjustedBy()).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void canonicalizesAdjustedByAcrossCaseAndWhitespaceForSameIdempotencyKey() {
        String sku = "ARC-SVC-IDEMP-9";
        var originalTransfer = createOriginalTransfer(sku, new BigDecimal("2"), "SO-IDEMP-9");

        String idempotencyKey = "reverse-svc-replay-9";
        var firstReversal = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "  Ops@ArcanaERP.com ", idempotencyKey)
        );

        var secondReplay = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "ops@arcanaerp.com", idempotencyKey)
        );

        var thirdReplay = inventoryAvailability.reverseTransfer(
            reverseCommand(originalTransfer.transferId(), DEFAULT_REASON, "OPS@ARCANAERP.COM", idempotencyKey)
        );

        assertReplayInvariants(secondReplay, firstReversal.transferId(), originalTransfer.transferId());
        assertReplayInvariants(thirdReplay, firstReversal.transferId(), originalTransfer.transferId());
        assertThat(secondReplay.adjustedBy()).isEqualTo(DEFAULT_ACTOR);
        assertThat(thirdReplay.adjustedBy()).isEqualTo(DEFAULT_ACTOR);

        var reversalHistory = assertSingleReversalHistoryContains(originalTransfer.transferId(), firstReversal.transferId());
        assertThat(reversalHistory.adjustedBy()).isEqualTo(DEFAULT_ACTOR);
    }

    private static ReverseInventoryTransferCommand reverseCommand(
        UUID transferId,
        String reason,
        String idempotencyKey
    ) {
        return reverseCommand(transferId, reason, DEFAULT_ACTOR, idempotencyKey);
    }

    private static ReverseInventoryTransferCommand reverseCommand(
        UUID transferId,
        String reason,
        String adjustedBy,
        String idempotencyKey
    ) {
        return new ReverseInventoryTransferCommand(transferId, reason, adjustedBy, idempotencyKey);
    }

    private InventoryTransferView assertSingleReversalHistoryContains(
        UUID originalTransferId,
        UUID expectedReversalTransferId
    ) {
        PageResult<InventoryTransferView> reversals =
            inventoryAvailability.listReversals(originalTransferId, new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);

        InventoryTransferView reversal = reversals.items().getFirst();
        assertThat(reversal.transferId()).isEqualTo(expectedReversalTransferId);
        return reversal;
    }

    private InventoryTransferView createOriginalTransfer(
        String sku,
        BigDecimal quantity,
        String referenceId
    ) {
        return InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            quantity,
            referenceId
        );
    }

    private void assertPayloadConflict(UUID transferId, Runnable operation) {
        assertThatThrownBy(operation::run)
            .isInstanceOf(ReversalIdempotencyPayloadConflictException.class)
            .hasMessage("Idempotency-Key already used with different reversal payload for transferId: " + transferId);
    }

    private void assertReplayInvariants(
        InventoryTransferView replayedReversal,
        UUID expectedTransferId,
        UUID originalTransferId
    ) {
        assertThat(replayedReversal.transferId()).isEqualTo(expectedTransferId);
        assertThat(replayedReversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayedReversal.referenceId()).isEqualTo(originalTransferId.toString());
    }

}
