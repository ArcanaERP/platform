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
    name = "inventory_location_metadata_change_audits",
    indexes = {
        @Index(name = "idx_ilmca_location_changed", columnList = "inventoryLocationId,changedAt"),
        @Index(name = "idx_ilmca_code_changed", columnList = "locationCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryLocationMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryLocationId;

    @Column(nullable = false, length = 64)
    private String locationCode;

    @Column(nullable = false, length = 255)
    private String previousName;

    @Column(nullable = false, length = 255)
    private String currentName;

    @Column(length = 64)
    private String previousFacilityTypeCode;

    @Column(length = 64)
    private String currentFacilityTypeCode;

    @Column(length = 255)
    private String previousAddressLine1;

    @Column(length = 255)
    private String currentAddressLine1;

    @Column(length = 255)
    private String previousAddressLine2;

    @Column(length = 255)
    private String currentAddressLine2;

    @Column(length = 128)
    private String previousCity;

    @Column(length = 128)
    private String currentCity;

    @Column(length = 64)
    private String previousRegionCode;

    @Column(length = 64)
    private String currentRegionCode;

    @Column(length = 32)
    private String previousPostalCode;

    @Column(length = 32)
    private String currentPostalCode;

    @Column(length = 2)
    private String previousCountryCode;

    @Column(length = 2)
    private String currentCountryCode;

    @Column(length = 128)
    private String previousContactName;

    @Column(length = 128)
    private String currentContactName;

    @Column(length = 255)
    private String previousContactEmail;

    @Column(length = 255)
    private String currentContactEmail;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryLocationMetadataChangeAudit create(
        UUID inventoryLocationId,
        String locationCode,
        InventoryLocationMetadataSnapshot previous,
        InventoryLocationMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (inventoryLocationId == null) {
            throw new IllegalArgumentException("inventoryLocationId is required");
        }
        if (previous == null) {
            throw new IllegalArgumentException("previous is required");
        }
        if (current == null) {
            throw new IllegalArgumentException("current is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryLocationMetadataChangeAudit audit = new InventoryLocationMetadataChangeAudit();
        audit.inventoryLocationId = inventoryLocationId;
        audit.locationCode = normalizeRequired(locationCode, "locationCode").toUpperCase();
        audit.previousName = normalizeRequired(previous.name(), "previousName");
        audit.currentName = normalizeRequired(current.name(), "currentName");
        audit.previousFacilityTypeCode = normalizeOptionalUpper(previous.facilityTypeCode());
        audit.currentFacilityTypeCode = normalizeOptionalUpper(current.facilityTypeCode());
        audit.previousAddressLine1 = normalizeOptional(previous.addressLine1());
        audit.currentAddressLine1 = normalizeOptional(current.addressLine1());
        audit.previousAddressLine2 = normalizeOptional(previous.addressLine2());
        audit.currentAddressLine2 = normalizeOptional(current.addressLine2());
        audit.previousCity = normalizeOptional(previous.city());
        audit.currentCity = normalizeOptional(current.city());
        audit.previousRegionCode = normalizeOptionalUpper(previous.regionCode());
        audit.currentRegionCode = normalizeOptionalUpper(current.regionCode());
        audit.previousPostalCode = normalizeOptional(previous.postalCode());
        audit.currentPostalCode = normalizeOptional(current.postalCode());
        audit.previousCountryCode = normalizeOptionalUpper(previous.countryCode());
        audit.currentCountryCode = normalizeOptionalUpper(current.countryCode());
        audit.previousContactName = normalizeOptional(previous.contactName());
        audit.currentContactName = normalizeOptional(current.contactName());
        audit.previousContactEmail = normalizeOptionalLower(previous.contactEmail());
        audit.currentContactEmail = normalizeOptionalLower(current.contactEmail());
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static String normalizeOptionalLower(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
