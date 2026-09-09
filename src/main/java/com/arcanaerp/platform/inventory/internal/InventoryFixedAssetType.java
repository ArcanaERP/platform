package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_fixed_asset_types",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_fixed_asset_types_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    static InventoryFixedAssetType create(String code, String description, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryFixedAssetType type = new InventoryFixedAssetType();
        type.code = normalizeRequired(code, "code").toUpperCase();
        type.description = normalizeRequired(description, "description");
        type.createdAt = createdAt;
        type.updatedAt = createdAt;
        return type;
    }

    void updateMetadata(String description, Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedDescription = normalizeRequired(description, "description");
        if (this.description.equals(normalizedDescription)) {
            throw new IllegalArgumentException("Inventory fixed asset type metadata is unchanged");
        }
        this.description = normalizedDescription;
        this.updatedAt = updatedAt;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
