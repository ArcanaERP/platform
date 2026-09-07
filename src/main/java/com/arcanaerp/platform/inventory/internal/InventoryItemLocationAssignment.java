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
    name = "inventory_item_location_assignments",
    indexes = {
        @Index(name = "idx_iila_item_valid", columnList = "inventoryItemId,validFrom,validThru"),
        @Index(name = "idx_iila_sku_item_location", columnList = "sku,itemLocationCode"),
        @Index(name = "idx_iila_assigned_location", columnList = "assignedLocationCode"),
        @Index(name = "idx_iila_active", columnList = "active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryItemLocationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String itemLocationCode;

    @Column(nullable = false, length = 64)
    private String assignedLocationCode;

    @Column(nullable = false)
    private Instant validFrom;

    private Instant validThru;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, length = 128)
    private String assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(length = 256)
    private String endReason;

    @Column(length = 128)
    private String endedBy;

    private Instant endedAt;

    static InventoryItemLocationAssignment create(
        InventoryItem item,
        InventoryLocation assignedLocation,
        Instant validFrom,
        String assignedBy,
        Instant assignedAt
    ) {
        if (item == null) {
            throw new IllegalArgumentException("inventory item is required");
        }
        if (assignedLocation == null) {
            throw new IllegalArgumentException("assigned location is required");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("validFrom is required");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }

        InventoryItemLocationAssignment assignment = new InventoryItemLocationAssignment();
        assignment.inventoryItemId = item.getId();
        assignment.sku = item.getSku();
        assignment.itemLocationCode = item.getLocationCode();
        assignment.assignedLocationCode = assignedLocation.getCode();
        assignment.validFrom = validFrom;
        assignment.active = true;
        assignment.assignedBy = normalizeRequired(assignedBy, "assignedBy").toLowerCase();
        assignment.assignedAt = assignedAt;
        return assignment;
    }

    void end(Instant validThru, String reason, String endedBy, Instant endedAt) {
        if (validThru == null) {
            throw new IllegalArgumentException("validThru is required");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt is required");
        }
        if (!active) {
            throw new IllegalArgumentException("Inventory item location assignment is already ended");
        }
        if (validThru.isBefore(validFrom)) {
            throw new IllegalArgumentException("validThru must be after or equal to validFrom");
        }

        this.validThru = validThru;
        active = false;
        endReason = normalizeRequired(reason, "reason");
        this.endedBy = normalizeRequired(endedBy, "endedBy").toLowerCase();
        this.endedAt = endedAt;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
