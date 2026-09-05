package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_locations",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_locations_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 64)
    private String facilityTypeCode;

    @Column(length = 255)
    private String addressLine1;

    @Column(length = 255)
    private String addressLine2;

    @Column(length = 128)
    private String city;

    @Column(length = 64)
    private String regionCode;

    @Column(length = 32)
    private String postalCode;

    @Column(length = 2)
    private String countryCode;

    @Column(length = 128)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private InventoryLocation(
        UUID id,
        String code,
        String name,
        String facilityTypeCode,
        String addressLine1,
        String addressLine2,
        String city,
        String regionCode,
        String postalCode,
        String countryCode,
        String contactName,
        String contactEmail,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.facilityTypeCode = facilityTypeCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.regionCode = regionCode;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static InventoryLocation create(String code, String name, Instant createdAt) {
        return create(code, name, null, null, null, null, null, null, null, null, null, createdAt);
    }

    static InventoryLocation create(
        String code,
        String name,
        String facilityTypeCode,
        String addressLine1,
        String addressLine2,
        String city,
        String regionCode,
        String postalCode,
        String countryCode,
        String contactName,
        String contactEmail,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new InventoryLocation(
            null,
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeOptionalUpper(facilityTypeCode),
            normalizeOptional(addressLine1),
            normalizeOptional(addressLine2),
            normalizeOptional(city),
            normalizeOptionalUpper(regionCode),
            normalizeOptional(postalCode),
            normalizeOptionalUpper(countryCode),
            normalizeOptional(contactName),
            normalizeOptionalLower(contactEmail),
            true,
            createdAt,
            createdAt
        );
    }

    void setActive(boolean active, Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        if (this.active == active) {
            throw new IllegalArgumentException("Inventory location active flag is already " + active);
        }
        this.active = active;
        this.updatedAt = updatedAt;
    }

    void updateMetadata(
        String name,
        String facilityTypeCode,
        String addressLine1,
        String addressLine2,
        String city,
        String regionCode,
        String postalCode,
        String countryCode,
        String contactName,
        String contactEmail,
        Instant updatedAt
    ) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedName = normalizeRequired(name, "name");
        String normalizedFacilityTypeCode = normalizeOptionalUpper(facilityTypeCode);
        String normalizedAddressLine1 = normalizeOptional(addressLine1);
        String normalizedAddressLine2 = normalizeOptional(addressLine2);
        String normalizedCity = normalizeOptional(city);
        String normalizedRegionCode = normalizeOptionalUpper(regionCode);
        String normalizedPostalCode = normalizeOptional(postalCode);
        String normalizedCountryCode = normalizeOptionalUpper(countryCode);
        String normalizedContactName = normalizeOptional(contactName);
        String normalizedContactEmail = normalizeOptionalLower(contactEmail);

        if (
            this.name.equals(normalizedName)
                && equalsNullable(this.facilityTypeCode, normalizedFacilityTypeCode)
                && equalsNullable(this.addressLine1, normalizedAddressLine1)
                && equalsNullable(this.addressLine2, normalizedAddressLine2)
                && equalsNullable(this.city, normalizedCity)
                && equalsNullable(this.regionCode, normalizedRegionCode)
                && equalsNullable(this.postalCode, normalizedPostalCode)
                && equalsNullable(this.countryCode, normalizedCountryCode)
                && equalsNullable(this.contactName, normalizedContactName)
                && equalsNullable(this.contactEmail, normalizedContactEmail)
        ) {
            throw new IllegalArgumentException("Inventory location metadata is unchanged");
        }

        this.name = normalizedName;
        this.facilityTypeCode = normalizedFacilityTypeCode;
        this.addressLine1 = normalizedAddressLine1;
        this.addressLine2 = normalizedAddressLine2;
        this.city = normalizedCity;
        this.regionCode = normalizedRegionCode;
        this.postalCode = normalizedPostalCode;
        this.countryCode = normalizedCountryCode;
        this.contactName = normalizedContactName;
        this.contactEmail = normalizedContactEmail;
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

    private static String normalizeOptionalLower(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
