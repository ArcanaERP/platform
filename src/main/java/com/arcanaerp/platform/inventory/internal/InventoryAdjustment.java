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
    name = "inventory_adjustments",
    indexes = {
        @Index(name = "idx_inventory_adjustments_item_time", columnList = "inventoryItemId,adjustedAt"),
        @Index(name = "idx_inventory_adjustments_item_actor_time", columnList = "inventoryItemId,adjustedBy,adjustedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryAdjustment {

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
    private BigDecimal previousOnHandQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentOnHandQuantity;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 128)
    private String adjustedBy;

    @Column(nullable = false, updatable = false)
    private Instant adjustedAt;

    private InventoryAdjustment(
        UUID id,
        UUID inventoryItemId,
        String sku,
        String locationCode,
        BigDecimal previousOnHandQuantity,
        BigDecimal quantityDelta,
        BigDecimal currentOnHandQuantity,
        String reason,
        String adjustedBy,
        Instant adjustedAt
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.sku = sku;
        this.locationCode = locationCode;
        this.previousOnHandQuantity = previousOnHandQuantity;
        this.quantityDelta = quantityDelta;
        this.currentOnHandQuantity = currentOnHandQuantity;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }

    static InventoryAdjustment create(
        UUID inventoryItemId,
        String sku,
        String locationCode,
        BigDecimal previousOnHandQuantity,
        BigDecimal quantityDelta,
        BigDecimal currentOnHandQuantity,
        String reason,
        String adjustedBy,
        Instant adjustedAt
    ) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        if (previousOnHandQuantity == null) {
            throw new IllegalArgumentException("previousOnHandQuantity is required");
        }
        if (quantityDelta == null || quantityDelta.signum() == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        if (currentOnHandQuantity == null || currentOnHandQuantity.signum() < 0) {
            throw new IllegalArgumentException("currentOnHandQuantity must be zero or greater");
        }
        if (adjustedAt == null) {
            throw new IllegalArgumentException("adjustedAt is required");
        }
        return new InventoryAdjustment(
            null,
            inventoryItemId,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            previousOnHandQuantity,
            quantityDelta,
            currentOnHandQuantity,
            normalizeRequired(reason, "reason"),
            normalizeRequired(adjustedBy, "adjustedBy").toLowerCase(),
            adjustedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
