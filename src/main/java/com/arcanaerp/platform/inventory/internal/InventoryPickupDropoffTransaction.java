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
    name = "inventory_pickup_dropoff_transactions",
    indexes = {
        @Index(name = "idx_inventory_pickup_dropoff_item_time", columnList = "inventoryItemId,transactionAt"),
        @Index(name = "idx_inventory_pickup_dropoff_sku_type_time", columnList = "sku,transactionTypeCode,transactionAt"),
        @Index(name = "idx_inventory_pickup_dropoff_ref_time", columnList = "sku,referenceType,referenceId,transactionAt"),
        @Index(name = "idx_inventory_pickup_dropoff_asset_time", columnList = "sku,fixedAssetCode,transactionAt"),
        @Index(name = "idx_inventory_pickup_dropoff_facility_time", columnList = "sku,facilityCode,transactionAt"),
        @Index(name = "idx_inventory_pickup_dropoff_adjustment", columnList = "inventoryAdjustmentId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryPickupDropoffTransaction {

    static final String PICKUP = "PICKUP";
    static final String DROPOFF = "DROPOFF";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false)
    private UUID inventoryAdjustmentId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, length = 32)
    private String transactionTypeCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal previousOnHandQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentOnHandQuantity;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 128)
    private String handledBy;

    @Column(length = 64)
    private String fixedAssetCode;

    @Column(length = 64)
    private String facilityCode;

    @Column(length = 64)
    private String referenceType;

    @Column(length = 128)
    private String referenceId;

    @Column(nullable = false, updatable = false)
    private Instant transactionAt;

    private InventoryPickupDropoffTransaction(
        UUID inventoryItemId,
        UUID inventoryAdjustmentId,
        String sku,
        String locationCode,
        String transactionTypeCode,
        BigDecimal quantity,
        BigDecimal quantityDelta,
        BigDecimal previousOnHandQuantity,
        BigDecimal currentOnHandQuantity,
        String reason,
        String handledBy,
        String fixedAssetCode,
        String facilityCode,
        String referenceType,
        String referenceId,
        Instant transactionAt
    ) {
        this.inventoryItemId = inventoryItemId;
        this.inventoryAdjustmentId = inventoryAdjustmentId;
        this.sku = sku;
        this.locationCode = locationCode;
        this.transactionTypeCode = transactionTypeCode;
        this.quantity = quantity;
        this.quantityDelta = quantityDelta;
        this.previousOnHandQuantity = previousOnHandQuantity;
        this.currentOnHandQuantity = currentOnHandQuantity;
        this.reason = reason;
        this.handledBy = handledBy;
        this.fixedAssetCode = fixedAssetCode;
        this.facilityCode = facilityCode;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.transactionAt = transactionAt;
    }

    static InventoryPickupDropoffTransaction create(
        UUID inventoryItemId,
        UUID inventoryAdjustmentId,
        String sku,
        String locationCode,
        String transactionTypeCode,
        BigDecimal quantity,
        BigDecimal previousOnHandQuantity,
        BigDecimal currentOnHandQuantity,
        String reason,
        String handledBy,
        String fixedAssetCode,
        String facilityCode,
        String referenceType,
        String referenceId,
        Instant transactionAt
    ) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        if (inventoryAdjustmentId == null) {
            throw new IllegalArgumentException("inventoryAdjustmentId is required");
        }
        String normalizedTransactionTypeCode = normalizeTransactionTypeCode(transactionTypeCode);
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (previousOnHandQuantity == null) {
            throw new IllegalArgumentException("previousOnHandQuantity is required");
        }
        if (currentOnHandQuantity == null || currentOnHandQuantity.signum() < 0) {
            throw new IllegalArgumentException("currentOnHandQuantity must be zero or greater");
        }
        if (transactionAt == null) {
            throw new IllegalArgumentException("transactionAt is required");
        }
        BigDecimal quantityDelta = quantityDeltaFor(normalizedTransactionTypeCode, quantity);
        return new InventoryPickupDropoffTransaction(
            inventoryItemId,
            inventoryAdjustmentId,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            normalizedTransactionTypeCode,
            quantity,
            quantityDelta,
            previousOnHandQuantity,
            currentOnHandQuantity,
            normalizeRequired(reason, "reason"),
            normalizeRequired(handledBy, "handledBy").toLowerCase(),
            normalizeOptionalUpper(fixedAssetCode),
            normalizeOptionalUpper(facilityCode),
            normalizeOptionalUpper(referenceType),
            normalizeOptional(referenceId),
            transactionAt
        );
    }

    static BigDecimal quantityDeltaFor(String transactionTypeCode, BigDecimal quantity) {
        String normalizedTransactionTypeCode = normalizeTransactionTypeCode(transactionTypeCode);
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return PICKUP.equals(normalizedTransactionTypeCode) ? quantity.negate() : quantity;
    }

    static String normalizeTransactionTypeCode(String transactionTypeCode) {
        String normalizedTransactionTypeCode = normalizeRequired(transactionTypeCode, "transactionTypeCode").toUpperCase();
        if (!PICKUP.equals(normalizedTransactionTypeCode) && !DROPOFF.equals(normalizedTransactionTypeCode)) {
            throw new IllegalArgumentException("transactionTypeCode must be PICKUP or DROPOFF");
        }
        return normalizedTransactionTypeCode;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
