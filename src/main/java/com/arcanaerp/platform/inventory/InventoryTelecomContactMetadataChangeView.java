package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryTelecomContactMetadataChangeView(
    UUID id,
    UUID telecomContactId,
    String ownerType,
    String ownerCode,
    String previousContactPurposeCode,
    String currentContactPurposeCode,
    String previousTelecomType,
    String currentTelecomType,
    String previousContactName,
    String currentContactName,
    String previousContactValue,
    String currentContactValue,
    String changedBy,
    Instant changedAt
) {
}
