package com.arcanaerp.platform.core.uom.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unit_of_measurements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UnitOfMeasurement {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column
    private String domain;

    @Column(length = 2000)
    private String comments;

    @Column(nullable = false)
    private Instant createdAt;

    static UnitOfMeasurement create(String code, String description, String domain, String comments, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        UnitOfMeasurement unit = new UnitOfMeasurement();
        unit.id = UUID.randomUUID();
        unit.code = normalizeCode(code);
        unit.description = normalizeRequired(description, "description");
        unit.domain = normalizeOptional(domain, true);
        unit.comments = normalizeOptional(comments, false);
        unit.createdAt = createdAt;
        return unit;
    }

    private static String normalizeCode(String code) {
        return normalizeRequired(code, "code").toUpperCase();
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value, boolean upperCase) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return upperCase ? normalized.toUpperCase() : normalized;
    }
}
