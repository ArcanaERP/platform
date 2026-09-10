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
    name = "work_effort_associations",
    indexes = {
        @Index(name = "idx_wea_association_type", columnList = "associationTypeCode"),
        @Index(name = "idx_wea_relationship_type", columnList = "relationshipTypeCode"),
        @Index(name = "idx_wea_from_effort", columnList = "tenantCode,fromEffortNumber"),
        @Index(name = "idx_wea_to_effort", columnList = "tenantCode,toEffortNumber"),
        @Index(name = "idx_wea_effective_from", columnList = "effectiveFrom"),
        @Index(name = "idx_wea_effective_thru", columnList = "effectiveThru")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String associationTypeCode;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private UUID fromWorkEffortId;

    @Column(nullable = false, length = 64)
    private String fromEffortNumber;

    @Column(nullable = false)
    private UUID toWorkEffortId;

    @Column(nullable = false, length = 64)
    private String toEffortNumber;

    @Column(length = 64)
    private String fromRoleTypeCode;

    @Column(length = 64)
    private String toRoleTypeCode;

    @Column(length = 64)
    private String relationshipTypeCode;

    private Instant effectiveFrom;

    private Instant effectiveThru;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortAssociation create(
        WorkEffortAssociationType associationType,
        String description,
        WorkEffort fromWorkEffort,
        WorkEffort toWorkEffort,
        String fromRoleTypeCode,
        String toRoleTypeCode,
        String relationshipTypeCode,
        Instant effectiveFrom,
        Instant effectiveThru,
        Instant createdAt
    ) {
        if (associationType == null) {
            throw new IllegalArgumentException("associationType is required");
        }
        if (fromWorkEffort == null) {
            throw new IllegalArgumentException("fromWorkEffort is required");
        }
        if (toWorkEffort == null) {
            throw new IllegalArgumentException("toWorkEffort is required");
        }
        if (!fromWorkEffort.getTenantCode().equals(toWorkEffort.getTenantCode())) {
            throw new IllegalArgumentException("associated work efforts must belong to the same tenant");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        if (effectiveFrom != null && effectiveThru != null && effectiveFrom.isAfter(effectiveThru)) {
            throw new IllegalArgumentException("effectiveFrom must be before or equal to effectiveThru");
        }
        WorkEffortAssociation association = new WorkEffortAssociation();
        association.tenantCode = fromWorkEffort.getTenantCode();
        association.associationTypeCode = associationType.getCode();
        association.description = normalizeOptional(description);
        association.fromWorkEffortId = fromWorkEffort.getId();
        association.fromEffortNumber = fromWorkEffort.getEffortNumber();
        association.toWorkEffortId = toWorkEffort.getId();
        association.toEffortNumber = toWorkEffort.getEffortNumber();
        association.fromRoleTypeCode = normalizeOptionalUpper(fromRoleTypeCode);
        association.toRoleTypeCode = normalizeOptionalUpper(toRoleTypeCode);
        association.relationshipTypeCode = normalizeOptionalUpper(relationshipTypeCode);
        association.effectiveFrom = effectiveFrom;
        association.effectiveThru = effectiveThru;
        association.createdAt = createdAt;
        return association;
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
