package com.arcanaerp.platform.workeffort.internal;

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
    name = "work_effort_association_types",
    indexes = {
        @Index(name = "idx_weat_code", columnList = "code", unique = true),
        @Index(name = "idx_weat_parent", columnList = "parentTypeCode"),
        @Index(name = "idx_weat_valid_from_role", columnList = "validFromRoleTypeCode"),
        @Index(name = "idx_weat_valid_to_role", columnList = "validToRoleTypeCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortAssociationType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 64)
    private String parentTypeCode;

    @Column(length = 64)
    private String validFromRoleTypeCode;

    @Column(length = 64)
    private String validToRoleTypeCode;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortAssociationType create(
        String code,
        String name,
        String description,
        String parentTypeCode,
        String validFromRoleTypeCode,
        String validToRoleTypeCode,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortAssociationType type = new WorkEffortAssociationType();
        type.code = normalizeRequired(code, "code").toUpperCase();
        type.name = normalizeRequired(name, "name");
        type.description = normalizeOptional(description);
        type.parentTypeCode = normalizeOptionalUpper(parentTypeCode);
        type.validFromRoleTypeCode = normalizeOptionalUpper(validFromRoleTypeCode);
        type.validToRoleTypeCode = normalizeOptionalUpper(validToRoleTypeCode);
        type.externalIdentifier = normalizeOptional(externalIdentifier);
        type.externalIdSource = normalizeOptional(externalIdSource);
        type.createdAt = createdAt;
        return type;
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

    private static String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
