package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_item_availability_change_audits",
    indexes = {
        @Index(name = "idx_iiaca_item_changed", columnList = "inventoryItemId,changedAt"),
        @Index(name = "idx_iiaca_sku_location_changed", columnList = "sku,locationCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryItemAvailabilityChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal previousAvailableQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAvailableQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableQuantityDelta;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal previousSoldQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentSoldQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal soldQuantityDelta;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private InventoryItemAvailabilityChangeAudit(
        UUID id,
        UUID inventoryItemId,
        String sku,
        String locationCode,
        BigDecimal previousAvailableQuantity,
        BigDecimal currentAvailableQuantity,
        BigDecimal availableQuantityDelta,
        BigDecimal previousSoldQuantity,
        BigDecimal currentSoldQuantity,
        BigDecimal soldQuantityDelta,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.sku = sku;
        this.locationCode = locationCode;
        this.previousAvailableQuantity = previousAvailableQuantity;
        this.currentAvailableQuantity = currentAvailableQuantity;
        this.availableQuantityDelta = availableQuantityDelta;
        this.previousSoldQuantity = previousSoldQuantity;
        this.currentSoldQuantity = currentSoldQuantity;
        this.soldQuantityDelta = soldQuantityDelta;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static InventoryItemAvailabilityChangeAudit create(
        UUID inventoryItemId,
        String sku,
        String locationCode,
        BigDecimal previousAvailableQuantity,
        BigDecimal currentAvailableQuantity,
        BigDecimal availableQuantityDelta,
        BigDecimal previousSoldQuantity,
        BigDecimal currentSoldQuantity,
        BigDecimal soldQuantityDelta,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new InventoryItemAvailabilityChangeAudit(
            null,
            inventoryItemId,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            normalizeRequiredQuantity(previousAvailableQuantity, "previousAvailableQuantity"),
            normalizeRequiredQuantity(currentAvailableQuantity, "currentAvailableQuantity"),
            normalizeRequiredQuantity(availableQuantityDelta, "availableQuantityDelta"),
            normalizeRequiredQuantity(previousSoldQuantity, "previousSoldQuantity"),
            normalizeRequiredQuantity(currentSoldQuantity, "currentSoldQuantity"),
            normalizeRequiredQuantity(soldQuantityDelta, "soldQuantityDelta"),
            normalizeRequired(reason, "reason"),
            normalizeRequired(changedBy, "changedBy").toLowerCase(),
            changedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal normalizeRequiredQuantity(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
