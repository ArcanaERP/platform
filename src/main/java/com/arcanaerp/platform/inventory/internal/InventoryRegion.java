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
    name = "inventory_regions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_regions_country_code",
        columnNames = {"countryCode", "code"}
    ),
    indexes = @Index(name = "idx_inventory_regions_country", columnList = "countryCode")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryRegion create(String countryCode, String code, String name, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryRegion region = new InventoryRegion();
        region.countryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();
        region.code = normalizeRequired(code, "code").toUpperCase();
        region.name = normalizeRequired(name, "name");
        region.createdAt = createdAt;
        return region;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
