package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal onHandQuantity;

    @Column(nullable = false)
    private Instant updatedAt;

    private InventoryItem(UUID id, String sku, String locationCode, BigDecimal onHandQuantity, Instant updatedAt) {
        this.id = id;
        this.sku = sku;
        this.locationCode = locationCode;
        this.onHandQuantity = onHandQuantity;
        this.updatedAt = updatedAt;
    }

    static InventoryItem create(String sku, String locationCode, BigDecimal onHandQuantity, Instant updatedAt) {
        if (onHandQuantity == null || onHandQuantity.signum() < 0) {
            throw new IllegalArgumentException("onHandQuantity must be zero or greater");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }

        return new InventoryItem(
            null,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            onHandQuantity,
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
        if (nextOnHand.signum() < 0) {
            throw new IllegalArgumentException("onHandQuantity cannot become negative");
        }

        this.onHandQuantity = nextOnHand;
        this.updatedAt = adjustedAt;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
