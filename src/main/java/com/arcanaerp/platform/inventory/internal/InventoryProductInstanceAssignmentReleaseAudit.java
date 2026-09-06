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
    name = "inventory_product_instance_assignment_release_audits",
    indexes = {
        @Index(name = "idx_ipiara_assignment_released", columnList = "assignmentId,releasedAt"),
        @Index(name = "idx_ipiara_product_instance", columnList = "productInstanceCode,releasedAt"),
        @Index(name = "idx_ipiara_released_by", columnList = "releasedBy,releasedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryProductInstanceAssignmentReleaseAudit {

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
    private String locationCode;

    @Column(nullable = false, length = 64)
    private String productInstanceCode;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, length = 128)
    private String releasedBy;

    @Column(nullable = false, updatable = false)
    private Instant releasedAt;

    static InventoryProductInstanceAssignmentReleaseAudit create(
        InventoryProductInstanceAssignment assignment,
        String reason,
        String releasedBy,
        Instant releasedAt
    ) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment is required");
        }
        if (releasedAt == null) {
            throw new IllegalArgumentException("releasedAt is required");
        }

        InventoryProductInstanceAssignmentReleaseAudit audit = new InventoryProductInstanceAssignmentReleaseAudit();
        audit.assignmentId = assignment.getId();
        audit.inventoryItemId = assignment.getInventoryItemId();
        audit.sku = assignment.getSku();
        audit.locationCode = assignment.getLocationCode();
        audit.productInstanceCode = assignment.getProductInstanceCode();
        audit.reason = normalizeRequired(reason, "reason");
        audit.releasedBy = normalizeRequired(releasedBy, "releasedBy").toLowerCase();
        audit.releasedAt = releasedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
