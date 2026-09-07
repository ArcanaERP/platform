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
    name = "inventory_facility_active_change_audits",
    indexes = {
        @Index(name = "idx_ifaca_facility_changed", columnList = "inventoryFacilityId,changedAt"),
        @Index(name = "idx_ifaca_code_changed", columnList = "facilityCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFacilityActiveChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryFacilityId;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false)
    private boolean previousActive;

    @Column(nullable = false)
    private boolean currentActive;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryFacilityActiveChangeAudit create(
        UUID inventoryFacilityId,
        String facilityCode,
        boolean previousActive,
        boolean currentActive,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryFacilityId == null) {
            throw new IllegalArgumentException("inventoryFacilityId is required");
        }
        if (previousActive == currentActive) {
            throw new IllegalArgumentException("Inventory facility active flag is unchanged");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryFacilityActiveChangeAudit audit = new InventoryFacilityActiveChangeAudit();
        audit.inventoryFacilityId = inventoryFacilityId;
        audit.facilityCode = normalizeRequired(facilityCode, "facilityCode").toUpperCase();
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
