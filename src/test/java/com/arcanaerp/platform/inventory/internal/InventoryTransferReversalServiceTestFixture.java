package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryTransferView;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

final class InventoryTransferReversalServiceTestFixture {

    private static final BigDecimal DEFAULT_MAIN_ON_HAND = new BigDecimal("20");
    private static final BigDecimal DEFAULT_EAST_ON_HAND = new BigDecimal("5");
    private static final Instant DEFAULT_SEEDED_AT = Instant.parse("2026-03-04T00:00:00Z");
    private static final String DEFAULT_TRANSFER_REASON = "Original transfer";
    private static final String DEFAULT_ACTOR = "ops@arcanaerp.com";
    private static final String DEFAULT_REFERENCE_TYPE = "order";

    private InventoryTransferReversalServiceTestFixture() {}

    static InventoryTransferView createOriginalTransfer(
        InventoryAvailability inventoryAvailability,
        InventoryItemRepository inventoryItemRepository,
        String sku,
        BigDecimal quantity,
        String referenceId
    ) {
        seedTransferItems(inventoryItemRepository, sku);
        return inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                "main",
                "wh-east",
                quantity,
                DEFAULT_TRANSFER_REASON,
                DEFAULT_ACTOR,
                DEFAULT_REFERENCE_TYPE,
                referenceId
            )
        );
    }

    static InventoryTransferView reverseTransfer(
        InventoryAvailability inventoryAvailability,
        UUID transferId,
        String reason,
        String adjustedBy,
        String idempotencyKey
    ) {
        return inventoryAvailability.reverseTransfer(
            new ReverseInventoryTransferCommand(transferId, reason, adjustedBy, idempotencyKey)
        );
    }

    static String fingerprintForReversalRequest(String reason, String adjustedBy) {
        String canonicalRequest = reason + "\n" + adjustedBy.toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private static void seedTransferItems(
        InventoryItemRepository inventoryItemRepository,
        String sku
    ) {
        InventoryIdempotencyTestFixture.seedTransferItems(
            inventoryItemRepository,
            sku,
            DEFAULT_MAIN_ON_HAND,
            DEFAULT_EAST_ON_HAND,
            DEFAULT_SEEDED_AT
        );
    }
}
