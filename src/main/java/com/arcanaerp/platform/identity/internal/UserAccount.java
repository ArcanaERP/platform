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
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "identity_users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_identity_users_tenant_email",
        columnNames = {"tenantId", "email"}
    ),
    indexes = {
        @Index(name = "idx_identity_users_tenant", columnList = "tenantId"),
        @Index(name = "idx_identity_users_role", columnList = "roleId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID roleId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private UserAccount(
        UUID id,
        UUID tenantId,
        UUID roleId,
        String email,
        String displayName,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.email = email;
        this.displayName = displayName;
        this.active = active;
        this.createdAt = createdAt;
    }

    static UserAccount create(UUID tenantId, UUID roleId, String email, String displayName, Instant createdAt) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("roleId is required");
        }
        return new UserAccount(
            null,
            tenantId,
            roleId,
            normalizeEmail(email),
            normalizeRequired(displayName, "displayName"),
            true,
            createdAt
        );
    }

    private static String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "email").toLowerCase();
        if (!SIMPLE_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("email is invalid");
        }
        return normalized;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
