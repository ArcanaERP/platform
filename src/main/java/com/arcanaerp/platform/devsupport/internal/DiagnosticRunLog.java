package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
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
    name = "dev_support_diagnostic_run_logs",
    uniqueConstraints = @UniqueConstraint(name = "uk_dev_support_run_tenant_number", columnNames = {"tenantCode", "runNumber"}),
    indexes = {
        @Index(name = "idx_dev_support_run_tenant_created", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_dev_support_run_tenant_status", columnList = "tenantCode,status"),
        @Index(name = "idx_dev_support_run_tenant_started", columnList = "tenantCode,startedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DiagnosticRunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String runNumber;

    @Column(nullable = false, length = 64)
    private String diagnosticCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiagnosticRunStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private DiagnosticRunLog(
        UUID id,
        String tenantCode,
        String runNumber,
        String diagnosticCode,
        String title,
        String summary,
        DiagnosticRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.runNumber = runNumber;
        this.diagnosticCode = diagnosticCode;
        this.title = title;
        this.summary = summary;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = createdAt;
    }

    static DiagnosticRunLog create(
        String tenantCode,
        String runNumber,
        String diagnosticCode,
        String title,
        String summary,
        DiagnosticRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt is required");
        }
        if (finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must be after or equal to startedAt");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new DiagnosticRunLog(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(runNumber, "runNumber").toUpperCase(),
            normalizeRequired(diagnosticCode, "diagnosticCode").toUpperCase(),
            normalizeRequired(title, "title"),
            normalizeRequired(summary, "summary"),
            status,
            startedAt,
            finishedAt,
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
