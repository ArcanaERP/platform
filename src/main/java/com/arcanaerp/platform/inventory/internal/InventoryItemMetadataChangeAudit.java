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
    name = "inventory_item_metadata_change_audits",
    indexes = {
        @Index(name = "idx_iimca_item_changed", columnList = "inventoryItemId,changedAt"),
        @Index(name = "idx_iimca_sku_location_changed", columnList = "sku,locationCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryItemMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryItemId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, length = 32)
    private String previousUnitOfMeasurementCode;

    @Column(nullable = false, length = 32)
    private String currentUnitOfMeasurementCode;

    @Column(nullable = false, length = 64)
    private String previousClassificationCode;

    @Column(nullable = false, length = 64)
    private String currentClassificationCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private InventoryItemMetadataChangeAudit(
        UUID id,
        UUID inventoryItemId,
        String sku,
        String locationCode,
        String previousUnitOfMeasurementCode,
        String currentUnitOfMeasurementCode,
        String previousClassificationCode,
        String currentClassificationCode,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.sku = sku;
        this.locationCode = locationCode;
        this.previousUnitOfMeasurementCode = previousUnitOfMeasurementCode;
        this.currentUnitOfMeasurementCode = currentUnitOfMeasurementCode;
        this.previousClassificationCode = previousClassificationCode;
        this.currentClassificationCode = currentClassificationCode;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static InventoryItemMetadataChangeAudit create(
        UUID inventoryItemId,
        String sku,
        String locationCode,
        String previousUnitOfMeasurementCode,
        String currentUnitOfMeasurementCode,
        String previousClassificationCode,
        String currentClassificationCode,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new InventoryItemMetadataChangeAudit(
            null,
            inventoryItemId,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(locationCode, "locationCode").toUpperCase(),
            normalizeRequired(previousUnitOfMeasurementCode, "previousUnitOfMeasurementCode").toUpperCase(),
            normalizeRequired(currentUnitOfMeasurementCode, "currentUnitOfMeasurementCode").toUpperCase(),
            normalizeRequired(previousClassificationCode, "previousClassificationCode").toUpperCase(),
            normalizeRequired(currentClassificationCode, "currentClassificationCode").toUpperCase(),
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
}
