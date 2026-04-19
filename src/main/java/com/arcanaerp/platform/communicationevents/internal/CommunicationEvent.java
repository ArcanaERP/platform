package com.arcanaerp.platform.communicationevents.internal;

import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
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
    name = "communication_events",
    uniqueConstraints = @UniqueConstraint(name = "uk_communication_events_tenant_event_number", columnNames = {"tenantCode", "eventNumber"}),
    indexes = {
        @Index(name = "idx_communication_events_tenant_occurred_at", columnList = "tenantCode, occurredAt"),
        @Index(name = "idx_communication_events_tenant_channel", columnList = "tenantCode, channel"),
        @Index(name = "idx_communication_events_tenant_direction", columnList = "tenantCode, direction"),
        @Index(name = "idx_communication_events_tenant_recorded_by", columnList = "tenantCode, recordedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CommunicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String eventNumber;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String statusCode;

    @Column(nullable = false, length = 255)
    private String statusName;

    @Column(nullable = false, length = 64)
    private String purposeCode;

    @Column(nullable = false, length = 255)
    private String purposeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunicationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommunicationDirection direction;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 320)
    private String recordedBy;

    @Column(length = 255)
    private String externalReference;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private CommunicationEvent(
        UUID id,
        String eventNumber,
        String tenantCode,
        String statusCode,
        String statusName,
        String purposeCode,
        String purposeName,
        CommunicationChannel channel,
        CommunicationDirection direction,
        String subject,
        String summary,
        Instant occurredAt,
        String recordedBy,
        String externalReference,
        Instant createdAt
    ) {
        this.id = id;
        this.eventNumber = eventNumber;
        this.tenantCode = tenantCode;
        this.statusCode = statusCode;
        this.statusName = statusName;
        this.purposeCode = purposeCode;
        this.purposeName = purposeName;
        this.channel = channel;
        this.direction = direction;
        this.subject = subject;
        this.summary = summary;
        this.occurredAt = occurredAt;
        this.recordedBy = recordedBy;
        this.externalReference = externalReference;
        this.createdAt = createdAt;
    }

    static CommunicationEvent create(
        String eventNumber,
        String tenantCode,
        String statusCode,
        String statusName,
        String purposeCode,
        String purposeName,
        CommunicationChannel channel,
        CommunicationDirection direction,
        String subject,
        String summary,
        Instant occurredAt,
        String recordedBy,
        String externalReference,
        Instant createdAt
    ) {
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new CommunicationEvent(
            null,
            normalizeRequired(eventNumber, "eventNumber").toUpperCase(),
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(statusCode, "statusCode").toUpperCase(),
            normalizeRequired(statusName, "statusName"),
            normalizeRequired(purposeCode, "purposeCode").toUpperCase(),
            normalizeRequired(purposeName, "purposeName"),
            channel,
            direction,
            normalizeRequired(subject, "subject"),
            normalizeRequired(summary, "summary"),
            occurredAt,
            normalizeEmail(recordedBy),
            normalizeOptional(externalReference),
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

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value, "recordedBy").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("recordedBy is invalid");
        }
        return normalized;
    }
}
