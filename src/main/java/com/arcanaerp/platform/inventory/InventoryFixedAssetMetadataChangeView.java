package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetMetadataChangeView(
    UUID id,
    String fixedAssetCode,
    String previousDescription,
    String currentDescription,
    String previousFixedAssetTypeCode,
    String currentFixedAssetTypeCode,
    String previousComments,
    String currentComments,
    String previousExternalIdentifier,
    String currentExternalIdentifier,
    String previousExternalIdSource,
    String currentExternalIdSource,
    String changedBy,
    Instant changedAt
) {
}
