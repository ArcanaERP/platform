package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_fixed_asset_metadata_change_audits",
    indexes = {
        @Index(name = "idx_ifamca_asset_changed", columnList = "inventoryFixedAssetId,changedAt"),
        @Index(name = "idx_ifamca_code_changed", columnList = "fixedAssetCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryFixedAssetId;

    @Column(nullable = false, length = 64)
    private String fixedAssetCode;

    @Column(nullable = false, length = 255)
    private String previousDescription;

    @Column(nullable = false, length = 255)
    private String currentDescription;

    @Column(length = 64)
    private String previousFixedAssetTypeCode;

    @Column(length = 64)
    private String currentFixedAssetTypeCode;

    @Column(length = 1024)
    private String previousComments;

    @Column(length = 1024)
    private String currentComments;

    @Column(length = 128)
    private String previousExternalIdentifier;

    @Column(length = 128)
    private String currentExternalIdentifier;

    @Column(length = 64)
    private String previousExternalIdSource;

    @Column(length = 64)
    private String currentExternalIdSource;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryFixedAssetMetadataChangeAudit create(
        UUID inventoryFixedAssetId,
        String fixedAssetCode,
        InventoryFixedAssetMetadataSnapshot previous,
        InventoryFixedAssetMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryFixedAssetId == null) {
            throw new IllegalArgumentException("inventoryFixedAssetId is required");
        }
        if (previous == null) {
            throw new IllegalArgumentException("previous is required");
        }
        if (current == null) {
            throw new IllegalArgumentException("current is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryFixedAssetMetadataChangeAudit audit = new InventoryFixedAssetMetadataChangeAudit();
        audit.inventoryFixedAssetId = inventoryFixedAssetId;
        audit.fixedAssetCode = normalizeRequired(fixedAssetCode, "fixedAssetCode").toUpperCase();
        audit.previousDescription = normalizeRequired(previous.description(), "previousDescription");
        audit.currentDescription = normalizeRequired(current.description(), "currentDescription");
        audit.previousFixedAssetTypeCode = normalizeOptionalUpper(previous.fixedAssetTypeCode());
        audit.currentFixedAssetTypeCode = normalizeOptionalUpper(current.fixedAssetTypeCode());
        audit.previousComments = normalizeOptional(previous.comments());
        audit.currentComments = normalizeOptional(current.comments());
        audit.previousExternalIdentifier = normalizeOptional(previous.externalIdentifier());
        audit.currentExternalIdentifier = normalizeOptional(current.externalIdentifier());
        audit.previousExternalIdSource = normalizeOptionalUpper(previous.externalIdSource());
        audit.currentExternalIdSource = normalizeOptionalUpper(current.externalIdSource());
        audit.changedBy = normalizeRequired(changedBy, "changedBy").toLowerCase();
        audit.changedAt = changedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
