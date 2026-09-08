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
    name = "inventory_countries",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_countries_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryCountry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryCountry create(String code, String name, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryCountry country = new InventoryCountry();
        country.code = normalizeRequired(code, "code").toUpperCase();
        country.name = normalizeRequired(name, "name");
        country.createdAt = createdAt;
        return country;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
