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
    name = "inventory_storage_areas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_storage_areas_facility_code",
        columnNames = {"facilityCode", "code"}
    ),
    indexes = {
        @Index(name = "idx_inventory_storage_areas_facility", columnList = "facilityCode"),
        @Index(name = "idx_inventory_storage_areas_type", columnList = "storageAreaType"),
        @Index(name = "idx_inventory_storage_areas_parent", columnList = "facilityCode,parentStorageAreaCode"),
        @Index(name = "idx_inventory_storage_areas_active", columnList = "active,facilityCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryStorageArea {

    static final String TYPE_AREA = "AREA";
    static final String TYPE_BIN = "BIN";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 32)
    private String storageAreaType;

    @Column(length = 64)
    private String parentStorageAreaCode;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    static InventoryStorageArea create(
        String facilityCode,
        String code,
        String name,
        String storageAreaType,
        String parentStorageAreaCode,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String normalizedParentCode = normalizeOptionalUpper(parentStorageAreaCode);
        if (normalizedCode.equals(normalizedParentCode)) {
            throw new IllegalArgumentException("parentStorageAreaCode must not match code");
        }

        InventoryStorageArea storageArea = new InventoryStorageArea();
        storageArea.facilityCode = normalizeRequired(facilityCode, "facilityCode").toUpperCase();
        storageArea.code = normalizedCode;
        storageArea.name = normalizeRequired(name, "name");
        storageArea.storageAreaType = normalizeStorageAreaType(storageAreaType);
        storageArea.parentStorageAreaCode = normalizedParentCode;
        storageArea.active = true;
        storageArea.createdAt = createdAt;
        storageArea.updatedAt = createdAt;
        return storageArea;
    }

    static String normalizeStorageAreaType(String value) {
        String normalized = normalizeRequired(value, "storageAreaType").toUpperCase();
        if (!TYPE_AREA.equals(normalized) && !TYPE_BIN.equals(normalized)) {
            throw new IllegalArgumentException("storageAreaType must be AREA or BIN");
        }
        return normalized;
    }

    void updateMetadata(String name, String storageAreaType, String parentStorageAreaCode, Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedName = normalizeRequired(name, "name");
        String normalizedStorageAreaType = normalizeStorageAreaType(storageAreaType);
        String normalizedParentStorageAreaCode = normalizeOptionalUpper(parentStorageAreaCode);
        if (code.equals(normalizedParentStorageAreaCode)) {
            throw new IllegalArgumentException("parentStorageAreaCode must not match code");
        }
        if (
            this.name.equals(normalizedName)
                && this.storageAreaType.equals(normalizedStorageAreaType)
                && equalsNullable(this.parentStorageAreaCode, normalizedParentStorageAreaCode)
        ) {
            throw new IllegalArgumentException("Inventory storage area metadata is unchanged");
        }
        this.name = normalizedName;
        this.storageAreaType = normalizedStorageAreaType;
        this.parentStorageAreaCode = normalizedParentStorageAreaCode;
        this.updatedAt = updatedAt;
    }

    void setActive(boolean active, Instant updatedAt) {
        if (this.active == active) {
            throw new IllegalArgumentException("Inventory storage area active flag is unchanged");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        this.active = active;
        this.updatedAt = updatedAt;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
