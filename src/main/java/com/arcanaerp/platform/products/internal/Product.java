package com.arcanaerp.platform.products.internal;

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
    name = "products",
    uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Product(UUID id, String sku, String name, UUID categoryId, boolean active, Instant createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.categoryId = categoryId;
        this.active = active;
        this.createdAt = createdAt;
    }

    static Product create(String sku, String name, UUID categoryId, Instant createdAt) {
        if (categoryId == null) {
            throw new IllegalArgumentException("categoryId is required");
        }
        return new Product(
            null,
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeRequired(name, "name"),
            categoryId,
            true,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
