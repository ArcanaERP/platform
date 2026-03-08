package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalStaleClaimIntegrationTest {

    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);

    @Autowired
    private InventoryAvailability inventoryAvailability;

    @Autowired
    private InventoryTransferReversalIdempotencyRepository reversalIdempotencyRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryTables() {
        reversalIdempotencyRepository.deleteAll();
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
    }

    @Test
    void reclaimsStalePendingClaimAndCreatesReversalWhenMissing() {
        String sku = "ARC-SVC-STALE-1";
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("3"),
            "SO-STALE-1"
        );

        String idempotencyKey = "reverse-stale-svc-1";
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransfer.transferId(),
                idempotencyKey,
                InventoryTransferReversalServiceTestFixture.fingerprintForReversalRequest(
                    "Reversal posted",
                    "ops@arcanaerp.com"
                ),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.parse("2025-12-01T00:00:00Z")
            )
        );

        var reversal = InventoryTransferReversalServiceTestFixture.reverseTransfer(
            inventoryAvailability,
            originalTransfer.transferId(),
            "Reversal posted",
            "ops@arcanaerp.com",
            idempotencyKey
        );

        assertThat(reversal.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(reversal.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransfer.transferId(), idempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(reversal.transferId());
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(reversal.transferId());
    }

    @Test
    void reclaimsStalePendingClaimAndLinksExistingReversalWhenAlreadyCreated() {
        String sku = "ARC-SVC-STALE-2";
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("3"),
            "SO-STALE-2"
        );

        var existingReversal = InventoryTransferReversalServiceTestFixture.reverseTransfer(
            inventoryAvailability,
            originalTransfer.transferId(),
            "Reversal posted",
            "ops@arcanaerp.com",
            null
        );

        String idempotencyKey = "reverse-stale-svc-2";
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransfer.transferId(),
                idempotencyKey,
                InventoryTransferReversalServiceTestFixture.fingerprintForReversalRequest(
                    "Reversal posted",
                    "ops@arcanaerp.com"
                ),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.parse("2025-12-01T00:00:00Z")
            )
        );

        var replayed = InventoryTransferReversalServiceTestFixture.reverseTransfer(
            inventoryAvailability,
            originalTransfer.transferId(),
            "Reversal posted",
            "ops@arcanaerp.com",
            idempotencyKey
        );

        assertThat(replayed.transferId()).isEqualTo(existingReversal.transferId());
        assertThat(replayed.referenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(replayed.referenceId()).isEqualTo(originalTransfer.transferId().toString());

        var idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransfer.transferId(), idempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(existingReversal.transferId());
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(1);
        assertThat(reversals.items().getFirst().transferId()).isEqualTo(existingReversal.transferId());
    }

}
