package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_postal_addresses",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_postal_addresses_owner_purpose",
        columnNames = {"ownerType", "ownerCode", "addressPurposeCode"}
    ),
    indexes = {
        @Index(name = "idx_inventory_postal_addresses_owner", columnList = "ownerType,ownerCode"),
        @Index(name = "idx_inventory_postal_addresses_purpose", columnList = "addressPurposeCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryPostalAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerCode;

    @Column(nullable = false, length = 64)
    private String addressPurposeCode;

    @Column(nullable = false, length = 255)
    private String addressLine1;

    @Column(length = 255)
    private String addressLine2;

    @Column(nullable = false, length = 128)
    private String city;

    @Column(length = 64)
    private String regionCode;

    @Column(nullable = false, length = 32)
    private String postalCode;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    static InventoryPostalAddress create(
        String ownerType,
        String ownerCode,
        String addressPurposeCode,
        String addressLine1,
        String addressLine2,
        String city,
        String regionCode,
        String postalCode,
        String countryCode,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryPostalAddress address = new InventoryPostalAddress();
        address.ownerType = normalizeRequired(ownerType, "ownerType").toUpperCase();
        address.ownerCode = normalizeRequired(ownerCode, "ownerCode").toUpperCase();
        address.addressPurposeCode = normalizeRequired(addressPurposeCode, "addressPurposeCode").toUpperCase();
        address.addressLine1 = normalizeRequired(addressLine1, "addressLine1");
        address.addressLine2 = normalizeOptional(addressLine2);
        address.city = normalizeRequired(city, "city");
        address.regionCode = normalizeOptionalUpper(regionCode);
        address.postalCode = normalizeRequired(postalCode, "postalCode");
        address.countryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();
        address.createdAt = createdAt;
        address.updatedAt = createdAt;
        return address;
    }

    void updateMetadata(
        String addressPurposeCode,
        String addressLine1,
        String addressLine2,
        String city,
        String regionCode,
        String postalCode,
        String countryCode,
        Instant updatedAt
    ) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedAddressPurposeCode = normalizeRequired(addressPurposeCode, "addressPurposeCode").toUpperCase();
        String normalizedAddressLine1 = normalizeRequired(addressLine1, "addressLine1");
        String normalizedAddressLine2 = normalizeOptional(addressLine2);
        String normalizedCity = normalizeRequired(city, "city");
        String normalizedRegionCode = normalizeOptionalUpper(regionCode);
        String normalizedPostalCode = normalizeRequired(postalCode, "postalCode");
        String normalizedCountryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();

        if (
            this.addressPurposeCode.equals(normalizedAddressPurposeCode)
                && this.addressLine1.equals(normalizedAddressLine1)
                && equalsNullable(this.addressLine2, normalizedAddressLine2)
                && this.city.equals(normalizedCity)
                && equalsNullable(this.regionCode, normalizedRegionCode)
                && this.postalCode.equals(normalizedPostalCode)
                && this.countryCode.equals(normalizedCountryCode)
        ) {
            throw new IllegalArgumentException("Inventory postal address metadata is unchanged");
        }

        this.addressPurposeCode = normalizedAddressPurposeCode;
        this.addressLine1 = normalizedAddressLine1;
        this.addressLine2 = normalizedAddressLine2;
        this.city = normalizedCity;
        this.regionCode = normalizedRegionCode;
        this.postalCode = normalizedPostalCode;
        this.countryCode = normalizedCountryCode;
        this.updatedAt = updatedAt;
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

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
