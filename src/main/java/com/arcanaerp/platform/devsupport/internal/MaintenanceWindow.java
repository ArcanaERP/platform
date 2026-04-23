package com.arcanaerp.platform.devsupport.internal;

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
    name = "dev_support_maintenance_windows",
    uniqueConstraints = @UniqueConstraint(name = "uk_dev_support_window_tenant_code", columnNames = {"tenantCode", "windowCode"}),
    indexes = {
        @Index(name = "idx_dev_support_window_tenant_created", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_dev_support_window_tenant_active", columnList = "tenantCode,active"),
        @Index(name = "idx_dev_support_window_tenant_starts", columnList = "tenantCode,startsAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class MaintenanceWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String windowCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private MaintenanceWindow(
        UUID id,
        String tenantCode,
        String windowCode,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.windowCode = windowCode;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
        this.createdAt = createdAt;
    }

    static MaintenanceWindow create(
        String tenantCode,
        String windowCode,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        Instant createdAt
    ) {
        if (startsAt == null) {
            throw new IllegalArgumentException("startsAt is required");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("endsAt is required");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new MaintenanceWindow(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(windowCode, "windowCode").toUpperCase(),
            normalizeRequired(title, "title"),
            normalizeRequired(description, "description"),
            startsAt,
            endsAt,
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
}
