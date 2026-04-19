package com.arcanaerp.platform.rules.internal;

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
    name = "rule_definitions",
    uniqueConstraints = @UniqueConstraint(name = "uk_rule_definitions_tenant_code", columnNames = {"tenantCode", "code"}),
    indexes = {
        @Index(name = "idx_rule_definitions_tenant_created_at", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_rule_definitions_tenant_applies_to", columnList = "tenantCode,appliesTo"),
        @Index(name = "idx_rule_definitions_tenant_active", columnList = "tenantCode,active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RuleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 64)
    private String appliesTo;

    @Column(nullable = false, length = 4000)
    private String expression;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private RuleDefinition(
        UUID id,
        String tenantCode,
        String code,
        String name,
        String appliesTo,
        String expression,
        String description,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.code = code;
        this.name = name;
        this.appliesTo = appliesTo;
        this.expression = expression;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
    }

    static RuleDefinition create(
        String tenantCode,
        String code,
        String name,
        String appliesTo,
        String expression,
        String description,
        boolean active,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new RuleDefinition(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeRequired(appliesTo, "appliesTo").toUpperCase(),
            normalizeRequired(expression, "expression"),
            normalizeOptional(description),
            active,
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
}
