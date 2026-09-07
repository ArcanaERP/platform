package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_items_sku_location",
        columnNames = {"sku", "locationCode"}
    ),
    indexes = {
        @Index(name = "idx_inventory_items_external_reference", columnList = "externalReference"),
        @Index(name = "idx_inventory_items_source_system", columnList = "sourceSystemCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {

    private static final String DEFAULT_UNIT_OF_MEASUREMENT_CODE = "EA";
    private static final String DEFAULT_CLASSIFICATION_CODE = "ON_HAND";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal onHandQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal soldQuantity;

    @Column(nullable = false, length = 32)
    private String unitOfMeasurementCode;

    @Column(nullable = false, length = 64)
    private String classificationCode;

    @Column(length = 128)
    private String productInstanceCode;

    @Column(length = 128)
    private String externalReference;

    @Column(length = 64)
    private String sourceSystemCode;

    @Column(nullable = false)
    private Instant updatedAt;

    private InventoryItem(
        UUID id,
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        BigDecimal soldQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        String externalReference,
        String sourceSystemCode,
        Instant updatedAt
    ) {
        this.id = id;
        this.sku = sku;
        this.locationCode = locationCode;
        this.onHandQuantity = onHandQuantity;
        this.availableQuantity = availableQuantity;
        this.soldQuantity = soldQuantity;
        this.unitOfMeasurementCode = unitOfMeasurementCode;
        this.classificationCode = classificationCode;
        this.productInstanceCode = productInstanceCode;
        this.externalReference = externalReference;
        this.sourceSystemCode = sourceSystemCode;
        this.updatedAt = updatedAt;
    }

    static InventoryItem create(String sku, String locationCode, BigDecimal onHandQuantity, Instant updatedAt) {
        return create(
            sku,
            locationCode,
            onHandQuantity,
            onHandQuantity,
            BigDecimal.ZERO,
            DEFAULT_UNIT_OF_MEASUREMENT_CODE,
            DEFAULT_CLASSIFICATION_CODE,
            null,
            updatedAt
        );
    }

    static InventoryItem create(
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        Instant updatedAt
    ) {
        return create(
            sku,
            locationCode,
            onHandQuantity,
            onHandQuantity,
            BigDecimal.ZERO,
            unitOfMeasurementCode,
            classificationCode,
            null,
            null,
            null,
            updatedAt
        );
    }

    static InventoryItem create(
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        String externalReference,
        String sourceSystemCode,
        Instant updatedAt
    ) {
        return create(
            sku,
            locationCode,
            onHandQuantity,
            onHandQuantity,
            BigDecimal.ZERO,
            unitOfMeasurementCode,
            classificationCode,
            productInstanceCode,
            externalReference,
            sourceSystemCode,
            updatedAt
        );
    }

    static InventoryItem create(
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        Instant updatedAt
    ) {
        return create(
            sku,
            locationCode,
            onHandQuantity,
            onHandQuantity,
            BigDecimal.ZERO,
            unitOfMeasurementCode,
            classificationCode,
            productInstanceCode,
            null,
            null,
            updatedAt
        );
    }

    static InventoryItem create(
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        BigDecimal soldQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        Instant updatedAt
    ) {
        return create(
            sku,
            locationCode,
            onHandQuantity,
            availableQuantity,
            soldQuantity,
            unitOfMeasurementCode,
            classificationCode,
            productInstanceCode,
            null,
            null,
            updatedAt
        );
    }

    static InventoryItem create(
        String sku,
        String locationCode,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        BigDecimal soldQuantity,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        String externalReference,
        String sourceSystemCode,
        Instant updatedAt
    ) {
        if (onHandQuantity == null || onHandQuantity.signum() < 0) {
            throw new IllegalArgumentException("onHandQuantity must be zero or greater");
        }
        if (availableQuantity == null || availableQuantity.signum() < 0) {
            throw new IllegalArgumentException("availableQuantity must be zero or greater");
        }
        if (soldQuantity == null || soldQuantity.signum() < 0) {
            throw new IllegalArgumentException("soldQuantity must be zero or greater");
        }
        if (availableQuantity.compareTo(onHandQuantity) > 0) {
            throw new IllegalArgumentException("availableQuantity must not exceed onHandQuantity");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }

        return new InventoryItem(
            null,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            onHandQuantity,
            availableQuantity,
            soldQuantity,
            normalizeRequired(unitOfMeasurementCode, "unitOfMeasurementCode").toUpperCase(),
            normalizeRequired(classificationCode, "classificationCode").toUpperCase(),
            normalizeOptionalUpper(productInstanceCode),
            normalizeOptional(externalReference),
            normalizeOptionalUpper(sourceSystemCode),
            updatedAt
        );
    }

    void applyAdjustment(BigDecimal quantityDelta, Instant adjustedAt) {
        if (quantityDelta == null) {
            throw new IllegalArgumentException("quantityDelta is required");
        }
        if (quantityDelta.signum() == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        if (adjustedAt == null) {
            throw new IllegalArgumentException("adjustedAt is required");
        }

        BigDecimal nextOnHand = onHandQuantity.add(quantityDelta);
        BigDecimal nextAvailable = availableQuantity.add(quantityDelta);
        if (nextOnHand.signum() < 0) {
            throw new IllegalArgumentException("onHandQuantity cannot become negative");
        }
        if (nextAvailable.signum() < 0) {
            throw new IllegalArgumentException("availableQuantity cannot become negative");
        }

        this.onHandQuantity = nextOnHand;
        this.availableQuantity = nextAvailable;
        this.updatedAt = adjustedAt;
    }

    void applyAvailabilityChange(
        BigDecimal availableQuantityDelta,
        BigDecimal soldQuantityDelta,
        Instant changedAt
    ) {
        if (availableQuantityDelta == null) {
            throw new IllegalArgumentException("availableQuantityDelta is required");
        }
        if (soldQuantityDelta == null) {
            throw new IllegalArgumentException("soldQuantityDelta is required");
        }
        if (availableQuantityDelta.signum() == 0 && soldQuantityDelta.signum() == 0) {
            throw new IllegalArgumentException("Inventory item availability is unchanged");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }

        BigDecimal nextAvailable = availableQuantity.add(availableQuantityDelta);
        BigDecimal nextSold = soldQuantity.add(soldQuantityDelta);
        if (nextAvailable.signum() < 0) {
            throw new IllegalArgumentException("availableQuantity cannot become negative");
        }
        if (nextSold.signum() < 0) {
            throw new IllegalArgumentException("soldQuantity cannot become negative");
        }
        if (nextAvailable.compareTo(onHandQuantity) > 0) {
            throw new IllegalArgumentException("availableQuantity must not exceed onHandQuantity");
        }

        this.availableQuantity = nextAvailable;
        this.soldQuantity = nextSold;
        this.updatedAt = changedAt;
    }

    void updateMetadata(
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        Instant updatedAt
    ) {
        updateMetadata(unitOfMeasurementCode, classificationCode, productInstanceCode, null, null, updatedAt);
    }

    void updateMetadata(
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        String externalReference,
        String sourceSystemCode,
        Instant updatedAt
    ) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedUnitOfMeasurementCode = normalizeRequired(unitOfMeasurementCode, "unitOfMeasurementCode").toUpperCase();
        String normalizedClassificationCode = normalizeRequired(classificationCode, "classificationCode").toUpperCase();
        String normalizedProductInstanceCode = normalizeOptionalUpper(productInstanceCode);
        String normalizedExternalReference = normalizeOptional(externalReference);
        String normalizedSourceSystemCode = normalizeOptionalUpper(sourceSystemCode);
        if (
            this.unitOfMeasurementCode.equals(normalizedUnitOfMeasurementCode)
                && this.classificationCode.equals(normalizedClassificationCode)
                && equalsNullable(this.productInstanceCode, normalizedProductInstanceCode)
                && equalsNullable(this.externalReference, normalizedExternalReference)
                && equalsNullable(this.sourceSystemCode, normalizedSourceSystemCode)
        ) {
            throw new IllegalArgumentException("Inventory item metadata is unchanged");
        }
        this.unitOfMeasurementCode = normalizedUnitOfMeasurementCode;
        this.classificationCode = normalizedClassificationCode;
        this.productInstanceCode = normalizedProductInstanceCode;
        this.externalReference = normalizedExternalReference;
        this.sourceSystemCode = normalizedSourceSystemCode;
        this.updatedAt = updatedAt;
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
