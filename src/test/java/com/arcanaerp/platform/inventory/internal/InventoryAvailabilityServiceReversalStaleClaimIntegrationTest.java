package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
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
                "SO-STALE-1"
            )
        );

        String idempotencyKey = "reverse-stale-svc-1";
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransfer.transferId(),
                idempotencyKey,
                fingerprintForReversalRequest("Reversal posted", "ops@arcanaerp.com"),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.parse("2025-12-01T00:00:00Z")
            )
        );

        var reversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
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
                "SO-STALE-2"
            )
        );

        var existingReversal = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                null
            )
        );

        String idempotencyKey = "reverse-stale-svc-2";
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransfer.transferId(),
                idempotencyKey,
                fingerprintForReversalRequest("Reversal posted", "ops@arcanaerp.com"),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.parse("2025-12-01T00:00:00Z")
            )
        );

        var replayed = inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(
                originalTransfer.transferId(),
                "Reversal posted",
                "ops@arcanaerp.com",
                idempotencyKey
            )
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

    private void seedTransferItems(String sku) {
        Instant seededAt = Instant.parse("2026-03-04T00:00:00Z");
        inventoryItemRepository.save(InventoryItem.create(sku, "main", new BigDecimal("20"), seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-east", new BigDecimal("5"), seededAt));
    }

    private static String fingerprintForReversalRequest(String reason, String adjustedBy) {
        String canonicalRequest = reason + "\n" + adjustedBy.toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
