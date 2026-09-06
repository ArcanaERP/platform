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
    name = "inventory_entry_role_types",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_entry_role_types_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryEntryRoleType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 1024)
    private String comments;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryEntryRoleType create(String code, String description, String comments, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryEntryRoleType type = new InventoryEntryRoleType();
        type.code = normalizeRequired(code, "code").toUpperCase();
        type.description = normalizeRequired(description, "description");
        type.comments = normalizeOptional(comments);
        type.createdAt = createdAt;
        return type;
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
}
