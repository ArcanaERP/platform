package com.arcanaerp.platform.communicationevents.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "communication_event_status_change_audits",
    indexes = {
        @Index(name = "idx_comm_evt_status_audit_event_changed", columnList = "communicationEventId,changedAt"),
        @Index(name = "idx_comm_evt_status_audit_event_tenant_changed", columnList = "communicationEventId,tenantCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CommunicationEventStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communicationEventId;

    @Column(nullable = false, length = 64)
    private String previousStatusCode;

    @Column(nullable = false, length = 255)
    private String previousStatusName;

    @Column(nullable = false, length = 64)
    private String currentStatusCode;

    @Column(nullable = false, length = 255)
    private String currentStatusName;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 320)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private CommunicationEventStatusChangeAudit(
        UUID id,
        UUID communicationEventId,
        String previousStatusCode,
        String previousStatusName,
        String currentStatusCode,
        String currentStatusName,
        String tenantCode,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.communicationEventId = communicationEventId;
        this.previousStatusCode = previousStatusCode;
        this.previousStatusName = previousStatusName;
        this.currentStatusCode = currentStatusCode;
        this.currentStatusName = currentStatusName;
        this.tenantCode = tenantCode;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static CommunicationEventStatusChangeAudit create(
        UUID communicationEventId,
        String previousStatusCode,
        String previousStatusName,
        String currentStatusCode,
        String currentStatusName,
        String tenantCode,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        if (communicationEventId == null) {
            throw new IllegalArgumentException("communicationEventId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new CommunicationEventStatusChangeAudit(
            null,
            communicationEventId,
            normalizeRequired(previousStatusCode, "previousStatusCode").toUpperCase(),
            normalizeRequired(previousStatusName, "previousStatusName"),
            normalizeRequired(currentStatusCode, "currentStatusCode").toUpperCase(),
            normalizeRequired(currentStatusName, "currentStatusName"),
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(reason, "reason"),
            normalizeRequired(changedBy, "changedBy").toLowerCase(),
            changedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
