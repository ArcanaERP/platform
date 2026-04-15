package com.arcanaerp.platform.identity.internal;

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
    name = "identity_roles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_identity_roles_tenant_code",
        columnNames = {"tenantId", "code"}
    ),
    indexes = @Index(name = "idx_identity_roles_tenant", columnList = "tenantId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Role(UUID id, UUID tenantId, String code, String name, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
    }

    static Role create(UUID tenantId, String code, String name, Instant createdAt) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return new Role(
            null,
            tenantId,
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
            createdAt
        );
    }

    void update(String name) {
        this.name = normalizeRequired(name, "name");
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
