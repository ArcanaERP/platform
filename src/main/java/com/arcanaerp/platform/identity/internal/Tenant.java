package com.arcanaerp.platform.identity.internal;

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
    name = "identity_tenants",
    uniqueConstraints = @UniqueConstraint(name = "uk_identity_tenants_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Tenant(UUID id, String code, String name, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
    }

    static Tenant create(String code, String name, Instant createdAt) {
        return new Tenant(null, normalizeCode(code), normalizeRequired(name, "name"), createdAt);
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
}
