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
    name = "inventory_location_type_metadata_change_audits",
    indexes = {
        @Index(name = "idx_iltmca_type_changed", columnList = "code,changedAt"),
        @Index(name = "idx_iltmca_changed_by", columnList = "changedBy,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryLocationTypeMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String previousDescription;

    @Column(nullable = false, length = 255)
    private String currentDescription;

    @Column(length = 64)
    private String previousParentCode;

    @Column(length = 64)
    private String currentParentCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryLocationTypeMetadataChangeAudit create(
        InventoryLocationType type,
        InventoryLocationTypeMetadataSnapshot previous,
        InventoryLocationTypeMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (previous == null) {
            throw new IllegalArgumentException("previous is required");
        }
        if (current == null) {
            throw new IllegalArgumentException("current is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryLocationTypeMetadataChangeAudit audit = new InventoryLocationTypeMetadataChangeAudit();
        audit.code = normalizeRequired(type.getCode(), "code").toUpperCase();
        audit.previousDescription = normalizeRequired(previous.description(), "previousDescription");
        audit.currentDescription = normalizeRequired(current.description(), "currentDescription");
        audit.previousParentCode = normalizeOptionalUpper(previous.parentCode());
        audit.currentParentCode = normalizeOptionalUpper(current.parentCode());
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

    private static String normalizeOptionalUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
