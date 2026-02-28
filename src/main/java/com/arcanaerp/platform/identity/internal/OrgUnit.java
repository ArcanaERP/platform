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
    name = "identity_org_units",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_identity_org_units_tenant_code",
        columnNames = {"tenantId", "code"}
    ),
    indexes = @Index(name = "idx_identity_org_units_tenant", columnList = "tenantId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private OrgUnit(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
    }

    static OrgUnit create(UUID tenantId, String code, String name, Instant createdAt) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return new OrgUnit(
            null,
            tenantId,
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
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
