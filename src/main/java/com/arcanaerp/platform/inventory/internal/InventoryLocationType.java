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
    name = "inventory_location_types",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_location_types_code", columnNames = "code"),
    indexes = @Index(name = "idx_inventory_location_types_parent", columnList = "parentCode")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryLocationType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 64)
    private String parentCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryLocationType create(String code, String description, Instant createdAt) {
        return create(code, description, null, createdAt);
    }

    static InventoryLocationType create(String code, String description, String parentCode, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String normalizedParentCode = normalizeOptionalUpper(parentCode);
        if (normalizedCode.equals(normalizedParentCode)) {
            throw new IllegalArgumentException("parentCode must not match code");
        }
        InventoryLocationType type = new InventoryLocationType();
        type.code = normalizedCode;
        type.description = normalizeRequired(description, "description");
        type.parentCode = normalizedParentCode;
        type.createdAt = createdAt;
        return type;
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
}
