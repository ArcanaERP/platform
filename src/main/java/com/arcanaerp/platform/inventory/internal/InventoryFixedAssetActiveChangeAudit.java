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
    name = "inventory_fixed_asset_active_change_audits",
    indexes = {
        @Index(name = "idx_ifaaca_asset_changed", columnList = "inventoryFixedAssetId,changedAt"),
        @Index(name = "idx_ifaaca_code_changed", columnList = "fixedAssetCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetActiveChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryFixedAssetId;

    @Column(nullable = false, length = 64)
    private String fixedAssetCode;

    @Column(nullable = false)
    private boolean previousActive;

    @Column(nullable = false)
    private boolean currentActive;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryFixedAssetActiveChangeAudit create(
        UUID inventoryFixedAssetId,
        String fixedAssetCode,
        boolean previousActive,
        boolean currentActive,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryFixedAssetId == null) {
            throw new IllegalArgumentException("inventoryFixedAssetId is required");
        }
        if (previousActive == currentActive) {
            throw new IllegalArgumentException("Inventory fixed asset active flag is unchanged");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryFixedAssetActiveChangeAudit audit = new InventoryFixedAssetActiveChangeAudit();
        audit.inventoryFixedAssetId = inventoryFixedAssetId;
        audit.fixedAssetCode = normalizeRequired(fixedAssetCode, "fixedAssetCode").toUpperCase();
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
