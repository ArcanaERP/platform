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
    name = "inventory_storage_area_metadata_change_audits",
    indexes = {
        @Index(name = "idx_isamca_area_changed", columnList = "storageAreaId,changedAt"),
        @Index(name = "idx_isamca_facility_changed", columnList = "facilityCode,storageAreaCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryStorageAreaMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID storageAreaId;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false, length = 64)
    private String storageAreaCode;

    @Column(nullable = false, length = 255)
    private String previousName;

    @Column(nullable = false, length = 255)
    private String currentName;

    @Column(nullable = false, length = 32)
    private String previousStorageAreaType;

    @Column(nullable = false, length = 32)
    private String currentStorageAreaType;

    @Column(length = 64)
    private String previousParentStorageAreaCode;

    @Column(length = 64)
    private String currentParentStorageAreaCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryStorageAreaMetadataChangeAudit create(
        InventoryStorageArea storageArea,
        InventoryStorageAreaMetadataSnapshot previous,
        InventoryStorageAreaMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (storageArea == null) {
            throw new IllegalArgumentException("storageArea is required");
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
        InventoryStorageAreaMetadataChangeAudit audit = new InventoryStorageAreaMetadataChangeAudit();
        audit.storageAreaId = storageArea.getId();
        audit.facilityCode = storageArea.getFacilityCode();
        audit.storageAreaCode = storageArea.getCode();
        audit.previousName = normalizeRequired(previous.name(), "previousName");
        audit.currentName = normalizeRequired(current.name(), "currentName");
        audit.previousStorageAreaType = InventoryStorageArea.normalizeStorageAreaType(previous.storageAreaType());
        audit.currentStorageAreaType = InventoryStorageArea.normalizeStorageAreaType(current.storageAreaType());
        audit.previousParentStorageAreaCode = normalizeOptionalUpper(previous.parentStorageAreaCode());
        audit.currentParentStorageAreaCode = normalizeOptionalUpper(current.parentStorageAreaCode());
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
