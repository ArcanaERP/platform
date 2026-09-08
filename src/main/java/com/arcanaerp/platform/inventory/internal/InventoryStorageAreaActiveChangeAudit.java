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
    name = "inventory_storage_area_active_change_audits",
    indexes = {
        @Index(name = "idx_isaaca_area_changed", columnList = "storageAreaId,changedAt"),
        @Index(name = "idx_isaaca_facility_changed", columnList = "facilityCode,storageAreaCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryStorageAreaActiveChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID storageAreaId;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false, length = 64)
    private String storageAreaCode;

    @Column(nullable = false)
    private boolean previousActive;

    @Column(nullable = false)
    private boolean currentActive;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryStorageAreaActiveChangeAudit create(
        InventoryStorageArea storageArea,
        boolean previousActive,
        boolean currentActive,
        String changedBy,
        Instant changedAt
    ) {
        if (storageArea == null) {
            throw new IllegalArgumentException("storageArea is required");
        }
        if (previousActive == currentActive) {
            throw new IllegalArgumentException("Inventory storage area active flag is unchanged");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryStorageAreaActiveChangeAudit audit = new InventoryStorageAreaActiveChangeAudit();
        audit.storageAreaId = storageArea.getId();
        audit.facilityCode = normalizeRequired(storageArea.getFacilityCode(), "facilityCode").toUpperCase();
        audit.storageAreaCode = normalizeRequired(storageArea.getCode(), "storageAreaCode").toUpperCase();
        audit.previousActive = previousActive;
        audit.currentActive = currentActive;
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
