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
    name = "inventory_fixed_asset_facility_assignment_type_metadata_change_audits",
    indexes = {
        @Index(name = "idx_ifafatmca_type_changed", columnList = "code,changedAt"),
        @Index(name = "idx_ifafatmca_changed_by", columnList = "changedBy,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String previousDescription;

    @Column(nullable = false, length = 255)
    private String currentDescription;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit create(
        InventoryFixedAssetFacilityAssignmentType type,
        InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot previous,
        InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot current,
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
        InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit audit =
            new InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit();
        audit.code = normalizeRequired(type.getCode(), "code").toUpperCase();
        audit.previousDescription = normalizeRequired(previous.description(), "previousDescription");
        audit.currentDescription = normalizeRequired(current.description(), "currentDescription");
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
