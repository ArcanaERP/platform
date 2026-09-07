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
    name = "inventory_facilities",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_facilities_code", columnNames = "code"),
    indexes = @Index(name = "idx_inventory_facilities_active_code", columnList = "active,code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

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

    private InventoryFacility(
        String code,
        String name,
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
        this.code = code;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.regionCode = regionCode;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    static InventoryFacility create(
        String code,
        String name,
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
        return new InventoryFacility(
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeOptional(addressLine1),
            normalizeOptional(addressLine2),
            normalizeOptional(city),
            normalizeOptionalUpper(regionCode),
            normalizeOptional(postalCode),
            normalizeOptionalUpper(countryCode),
            normalizeOptional(contactName),
            normalizeOptionalLower(contactEmail),
            createdAt
        );
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
