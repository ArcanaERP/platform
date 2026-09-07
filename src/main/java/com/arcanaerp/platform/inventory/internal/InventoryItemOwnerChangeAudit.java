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
    name = "inventory_item_owner_change_audits",
    indexes = {
        @Index(name = "idx_iioca_item_changed", columnList = "inventoryItemId,changedAt"),
        @Index(name = "idx_iioca_sku_location_changed", columnList = "sku,locationCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryItemOwnerChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(length = 64)
    private String previousOwnerTenantCode;

    @Column(length = 64)
    private String currentOwnerTenantCode;

    private UUID previousOwnerUserId;

    private UUID currentOwnerUserId;

    @Column(length = 64)
    private String previousOwnerRoleCode;

    @Column(length = 64)
    private String currentOwnerRoleCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryItemOwnerChangeAudit create(
        UUID inventoryItemId,
        String sku,
        String locationCode,
        String previousOwnerTenantCode,
        String currentOwnerTenantCode,
        UUID previousOwnerUserId,
        UUID currentOwnerUserId,
        String previousOwnerRoleCode,
        String currentOwnerRoleCode,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryItemOwnerChangeAudit audit = new InventoryItemOwnerChangeAudit();
        audit.inventoryItemId = inventoryItemId;
        audit.sku = normalizeRequired(sku, "sku").toUpperCase();
        audit.locationCode = normalizeRequired(locationCode, "locationCode").toUpperCase();
        audit.previousOwnerTenantCode = normalizeOptionalUpper(previousOwnerTenantCode);
        audit.currentOwnerTenantCode = normalizeOptionalUpper(currentOwnerTenantCode);
        audit.previousOwnerUserId = previousOwnerUserId;
        audit.currentOwnerUserId = currentOwnerUserId;
        audit.previousOwnerRoleCode = normalizeOptionalUpper(previousOwnerRoleCode);
        audit.currentOwnerRoleCode = normalizeOptionalUpper(currentOwnerRoleCode);
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
