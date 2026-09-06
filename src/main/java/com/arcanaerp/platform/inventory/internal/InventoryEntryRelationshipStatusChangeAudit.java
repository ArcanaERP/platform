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
    name = "inventory_entry_relationship_status_change_audits",
    indexes = {
        @Index(name = "idx_ier_status_audits_relationship_changed", columnList = "relationshipId,changedAt"),
        @Index(name = "idx_ier_status_audits_changed_by", columnList = "changedBy,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryEntryRelationshipStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID relationshipId;

    @Column(nullable = false, length = 64)
    private String previousStatusCode;

    @Column(nullable = false, length = 64)
    private String currentStatusCode;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryEntryRelationshipStatusChangeAudit create(
        UUID relationshipId,
        String previousStatusCode,
        String currentStatusCode,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        if (relationshipId == null) {
            throw new IllegalArgumentException("relationshipId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }

        InventoryEntryRelationshipStatusChangeAudit audit = new InventoryEntryRelationshipStatusChangeAudit();
        audit.relationshipId = relationshipId;
        audit.previousStatusCode = normalizeRequired(previousStatusCode, "previousStatusCode").toUpperCase();
        audit.currentStatusCode = normalizeRequired(currentStatusCode, "currentStatusCode").toUpperCase();
        audit.reason = normalizeRequired(reason, "reason");
        audit.changedBy = normalizeRequired(changedBy, "changedBy").toLowerCase();
        audit.changedAt = changedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
