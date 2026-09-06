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
    name = "inventory_entry_relationships",
    indexes = {
        @Index(name = "idx_inventory_entry_relationships_type", columnList = "relationshipTypeCode"),
        @Index(name = "idx_inventory_entry_relationships_from", columnList = "fromSku,fromLocationCode"),
        @Index(name = "idx_inventory_entry_relationships_to", columnList = "toSku,toLocationCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryEntryRelationship {

    private static final String DEFAULT_STATUS_CODE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String relationshipTypeCode;

    @Column(nullable = false)
    private UUID fromInventoryItemId;

    @Column(nullable = false, length = 64)
    private String fromSku;

    @Column(nullable = false, length = 64)
    private String fromLocationCode;

    @Column(nullable = false)
    private UUID toInventoryItemId;

    @Column(nullable = false, length = 64)
    private String toSku;

    @Column(nullable = false, length = 64)
    private String toLocationCode;

    @Column(nullable = false, length = 64)
    private String fromRoleTypeCode;

    @Column(nullable = false, length = 64)
    private String toRoleTypeCode;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 64)
    private String statusCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryEntryRelationship create(
        String relationshipTypeCode,
        InventoryItem fromItem,
        InventoryItem toItem,
        String fromRoleTypeCode,
        String toRoleTypeCode,
        String description,
        String statusCode,
        Instant createdAt
    ) {
        if (fromItem == null) {
            throw new IllegalArgumentException("from inventory item is required");
        }
        if (toItem == null) {
            throw new IllegalArgumentException("to inventory item is required");
        }
        if (fromItem.getId().equals(toItem.getId())) {
            throw new IllegalArgumentException("from and to inventory items must be different");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }

        InventoryEntryRelationship relationship = new InventoryEntryRelationship();
        relationship.relationshipTypeCode = normalizeRequired(relationshipTypeCode, "relationshipTypeCode").toUpperCase();
        relationship.fromInventoryItemId = fromItem.getId();
        relationship.fromSku = fromItem.getSku();
        relationship.fromLocationCode = fromItem.getLocationCode();
        relationship.toInventoryItemId = toItem.getId();
        relationship.toSku = toItem.getSku();
        relationship.toLocationCode = toItem.getLocationCode();
        relationship.fromRoleTypeCode = normalizeRequired(fromRoleTypeCode, "fromRoleTypeCode").toUpperCase();
        relationship.toRoleTypeCode = normalizeRequired(toRoleTypeCode, "toRoleTypeCode").toUpperCase();
        relationship.description = normalizeRequired(description, "description");
        relationship.statusCode = normalizeOptionalCode(statusCode, DEFAULT_STATUS_CODE);
        relationship.createdAt = createdAt;
        return relationship;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalCode(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }
}
