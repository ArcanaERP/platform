package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "dev_support_system_notices",
    uniqueConstraints = @UniqueConstraint(name = "uk_dev_support_notice_tenant_code", columnNames = {"tenantCode", "noticeCode"}),
    indexes = {
        @Index(name = "idx_dev_support_notice_tenant_created", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_dev_support_notice_tenant_severity", columnList = "tenantCode,severity"),
        @Index(name = "idx_dev_support_notice_tenant_active", columnList = "tenantCode,active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SystemNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String noticeCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NoticeSeverity severity;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private SystemNotice(
        UUID id,
        String tenantCode,
        String noticeCode,
        String title,
        String message,
        NoticeSeverity severity,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.noticeCode = noticeCode;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.active = active;
        this.createdAt = createdAt;
    }

    static SystemNotice create(
        String tenantCode,
        String noticeCode,
        String title,
        String message,
        NoticeSeverity severity,
        boolean active,
        Instant createdAt
    ) {
        if (severity == null) {
            throw new IllegalArgumentException("severity is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new SystemNotice(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(noticeCode, "noticeCode").toUpperCase(),
            normalizeRequired(title, "title"),
            normalizeRequired(message, "message"),
            severity,
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
