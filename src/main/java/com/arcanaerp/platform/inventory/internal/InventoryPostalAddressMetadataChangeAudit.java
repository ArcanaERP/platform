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
    name = "inventory_postal_address_metadata_change_audits",
    indexes = {
        @Index(name = "idx_ipamca_address_changed", columnList = "postalAddressId,changedAt"),
        @Index(name = "idx_ipamca_owner_changed", columnList = "ownerType,ownerCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryPostalAddressMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID postalAddressId;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerCode;

    @Column(nullable = false, length = 64)
    private String previousAddressPurposeCode;

    @Column(nullable = false, length = 64)
    private String currentAddressPurposeCode;

    @Column(nullable = false, length = 255)
    private String previousAddressLine1;

    @Column(nullable = false, length = 255)
    private String currentAddressLine1;

    @Column(length = 255)
    private String previousAddressLine2;

    @Column(length = 255)
    private String currentAddressLine2;

    @Column(nullable = false, length = 128)
    private String previousCity;

    @Column(nullable = false, length = 128)
    private String currentCity;

    @Column(length = 64)
    private String previousRegionCode;

    @Column(length = 64)
    private String currentRegionCode;

    @Column(nullable = false, length = 32)
    private String previousPostalCode;

    @Column(nullable = false, length = 32)
    private String currentPostalCode;

    @Column(nullable = false, length = 2)
    private String previousCountryCode;

    @Column(nullable = false, length = 2)
    private String currentCountryCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryPostalAddressMetadataChangeAudit create(
        UUID postalAddressId,
        String ownerType,
        String ownerCode,
        InventoryPostalAddressMetadataSnapshot previous,
        InventoryPostalAddressMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (postalAddressId == null) {
            throw new IllegalArgumentException("postalAddressId is required");
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
        InventoryPostalAddressMetadataChangeAudit audit = new InventoryPostalAddressMetadataChangeAudit();
        audit.postalAddressId = postalAddressId;
        audit.ownerType = normalizeRequired(ownerType, "ownerType").toUpperCase();
        audit.ownerCode = normalizeRequired(ownerCode, "ownerCode").toUpperCase();
        audit.previousAddressPurposeCode = normalizeRequired(previous.addressPurposeCode(), "previousAddressPurposeCode").toUpperCase();
        audit.currentAddressPurposeCode = normalizeRequired(current.addressPurposeCode(), "currentAddressPurposeCode").toUpperCase();
        audit.previousAddressLine1 = normalizeRequired(previous.addressLine1(), "previousAddressLine1");
        audit.currentAddressLine1 = normalizeRequired(current.addressLine1(), "currentAddressLine1");
        audit.previousAddressLine2 = normalizeOptional(previous.addressLine2());
        audit.currentAddressLine2 = normalizeOptional(current.addressLine2());
        audit.previousCity = normalizeRequired(previous.city(), "previousCity");
        audit.currentCity = normalizeRequired(current.city(), "currentCity");
        audit.previousRegionCode = normalizeOptionalUpper(previous.regionCode());
        audit.currentRegionCode = normalizeOptionalUpper(current.regionCode());
        audit.previousPostalCode = normalizeRequired(previous.postalCode(), "previousPostalCode");
        audit.currentPostalCode = normalizeRequired(current.postalCode(), "currentPostalCode");
        audit.previousCountryCode = normalizeRequired(previous.countryCode(), "previousCountryCode").toUpperCase();
        audit.currentCountryCode = normalizeRequired(current.countryCode(), "currentCountryCode").toUpperCase();
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
}
