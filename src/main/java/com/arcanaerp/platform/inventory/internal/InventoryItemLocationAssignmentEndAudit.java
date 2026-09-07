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
    name = "inventory_item_location_assignment_end_audits",
    indexes = {
        @Index(name = "idx_iilaea_assignment_ended", columnList = "assignmentId,endedAt"),
        @Index(name = "idx_iilaea_sku_location", columnList = "sku,itemLocationCode,endedAt"),
        @Index(name = "idx_iilaea_ended_by", columnList = "endedBy,endedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryItemLocationAssignmentEndAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID assignmentId;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String itemLocationCode;

    @Column(nullable = false, length = 64)
    private String assignedLocationCode;

    private Instant previousValidThru;

    @Column(nullable = false)
    private Instant currentValidThru;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, length = 128)
    private String endedBy;

    @Column(nullable = false, updatable = false)
    private Instant endedAt;

    static InventoryItemLocationAssignmentEndAudit create(
        InventoryItemLocationAssignment assignment,
        Instant previousValidThru,
        String reason,
        String endedBy,
        Instant endedAt
    ) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment is required");
        }
        if (assignment.getValidThru() == null) {
            throw new IllegalArgumentException("currentValidThru is required");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt is required");
        }

        InventoryItemLocationAssignmentEndAudit audit = new InventoryItemLocationAssignmentEndAudit();
        audit.assignmentId = assignment.getId();
        audit.inventoryItemId = assignment.getInventoryItemId();
        audit.sku = assignment.getSku();
        audit.itemLocationCode = assignment.getItemLocationCode();
        audit.assignedLocationCode = assignment.getAssignedLocationCode();
        audit.previousValidThru = previousValidThru;
        audit.currentValidThru = assignment.getValidThru();
        audit.reason = normalizeRequired(reason, "reason");
        audit.endedBy = normalizeRequired(endedBy, "endedBy").toLowerCase();
        audit.endedAt = endedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
