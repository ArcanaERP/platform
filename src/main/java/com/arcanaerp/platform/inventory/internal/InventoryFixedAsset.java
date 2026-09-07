package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_fixed_assets",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_fixed_assets_code", columnNames = "code"),
    indexes = @Index(name = "idx_inventory_fixed_assets_active_code", columnList = "active,code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryFixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 64)
    private String fixedAssetTypeCode;

    @Column(length = 1024)
    private String comments;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 64)
    private String externalIdSource;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private InventoryFixedAsset(
        String code,
        String description,
        String fixedAssetTypeCode,
        String comments,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        this.code = code;
        this.description = description;
        this.fixedAssetTypeCode = fixedAssetTypeCode;
        this.comments = comments;
        this.externalIdentifier = externalIdentifier;
        this.externalIdSource = externalIdSource;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    static InventoryFixedAsset create(
        String code,
        String description,
        String fixedAssetTypeCode,
        String comments,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new InventoryFixedAsset(
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(description, "description"),
            normalizeOptionalUpper(fixedAssetTypeCode),
            normalizeOptional(comments),
            normalizeOptional(externalIdentifier),
            normalizeOptionalUpper(externalIdSource),
            createdAt
        );
    }

    void setActive(boolean active, Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        if (this.active == active) {
            throw new IllegalArgumentException("Inventory fixed asset active flag is already " + active);
        }
        this.active = active;
        this.updatedAt = updatedAt;
    }

    void updateMetadata(
        String description,
        String fixedAssetTypeCode,
        String comments,
        String externalIdentifier,
        String externalIdSource,
        Instant updatedAt
    ) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedDescription = normalizeRequired(description, "description");
        String normalizedFixedAssetTypeCode = normalizeOptionalUpper(fixedAssetTypeCode);
        String normalizedComments = normalizeOptional(comments);
        String normalizedExternalIdentifier = normalizeOptional(externalIdentifier);
        String normalizedExternalIdSource = normalizeOptionalUpper(externalIdSource);

        if (
            this.description.equals(normalizedDescription)
                && equalsNullable(this.fixedAssetTypeCode, normalizedFixedAssetTypeCode)
                && equalsNullable(this.comments, normalizedComments)
                && equalsNullable(this.externalIdentifier, normalizedExternalIdentifier)
                && equalsNullable(this.externalIdSource, normalizedExternalIdSource)
        ) {
            throw new IllegalArgumentException("Inventory fixed asset metadata is unchanged");
        }

        this.description = normalizedDescription;
        this.fixedAssetTypeCode = normalizedFixedAssetTypeCode;
        this.comments = normalizedComments;
        this.externalIdentifier = normalizedExternalIdentifier;
        this.externalIdSource = normalizedExternalIdSource;
        this.updatedAt = updatedAt;
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

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
