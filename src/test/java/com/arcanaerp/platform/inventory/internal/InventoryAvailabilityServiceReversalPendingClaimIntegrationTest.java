package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.ReversalIdempotencyRaceConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryAvailabilityServiceReversalPendingClaimIntegrationTest {

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
    void keepsFreshPendingClaimInProcessingState() {
        String sku = "ARC-SVC-PENDING-1";
        var originalTransfer = InventoryTransferReversalServiceTestFixture.createOriginalTransfer(
            inventoryAvailability,
            inventoryItemRepository,
            sku,
            new BigDecimal("3"),
            "SO-PENDING-1"
        );

        String idempotencyKey = "reverse-pending-svc-1";
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransfer.transferId(),
                idempotencyKey,
                InventoryTransferReversalServiceTestFixture.fingerprintForReversalRequest(
                    "Reversal posted",
                    "ops@arcanaerp.com"
                ),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.now()
            )
        );

        assertThatThrownBy(() -> InventoryTransferReversalServiceTestFixture.reverseTransfer(
                inventoryAvailability,
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            ))
            .isInstanceOf(ReversalIdempotencyRaceConflictException.class)
            .hasMessage("Idempotency-Key is already being processed for transferId: " + originalTransfer.transferId());

        var idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransfer.transferId(), idempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        var reversals = inventoryAvailability.listReversals(originalTransfer.transferId(), new PageQuery(0, 10));
        assertThat(reversals.totalItems()).isEqualTo(0);
    }
}
