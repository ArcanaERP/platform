package com.arcanaerp.platform.communicationevents.internal;

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
    name = "communication_event_purpose_types",
    uniqueConstraints = @UniqueConstraint(name = "uk_comm_event_purpose_types_tenant_code", columnNames = {"tenantCode", "code"}),
    indexes = @Index(name = "idx_comm_event_purpose_types_tenant", columnList = "tenantCode")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CommunicationEventPurposeType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private CommunicationEventPurposeType(UUID id, String tenantCode, String code, String name, Instant createdAt) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
    }

    static CommunicationEventPurposeType create(String tenantCode, String code, String name, Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new CommunicationEventPurposeType(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(code, "code").toUpperCase(),
            normalizeRequired(name, "name"),
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
