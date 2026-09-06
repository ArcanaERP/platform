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
    name = "inventory_product_instance_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_product_instance_assignments_item_instance",
        columnNames = {"inventoryItemId", "productInstanceCode"}
    ),
    indexes = {
        @Index(name = "idx_ipia_product_instance", columnList = "productInstanceCode"),
        @Index(name = "idx_ipia_sku_location", columnList = "sku,locationCode"),
        @Index(name = "idx_ipia_assigned_by", columnList = "assignedBy,assignedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryProductInstanceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, length = 64)
    private String productInstanceCode;

    @Column(nullable = false, length = 128)
    private String assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 256)
    private String releaseReason;

    @Column(length = 128)
    private String releasedBy;

    private Instant releasedAt;

    static InventoryProductInstanceAssignment create(
        InventoryItem item,
        String productInstanceCode,
        String assignedBy,
        Instant assignedAt
    ) {
        if (item == null) {
            throw new IllegalArgumentException("inventory item is required");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }

        InventoryProductInstanceAssignment assignment = new InventoryProductInstanceAssignment();
        assignment.inventoryItemId = item.getId();
        assignment.sku = item.getSku();
        assignment.locationCode = item.getLocationCode();
        assignment.productInstanceCode = normalizeRequired(productInstanceCode, "productInstanceCode").toUpperCase();
        assignment.assignedBy = normalizeRequired(assignedBy, "assignedBy").toLowerCase();
        assignment.assignedAt = assignedAt;
        assignment.active = true;
        return assignment;
    }

    void release(String reason, String releasedBy, Instant releasedAt) {
        if (releasedAt == null) {
            throw new IllegalArgumentException("releasedAt is required");
        }
        if (!active) {
            throw new IllegalArgumentException("Inventory product instance assignment is already released");
        }
        active = false;
        releaseReason = normalizeRequired(reason, "reason");
        this.releasedBy = normalizeRequired(releasedBy, "releasedBy").toLowerCase();
        this.releasedAt = releasedAt;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
