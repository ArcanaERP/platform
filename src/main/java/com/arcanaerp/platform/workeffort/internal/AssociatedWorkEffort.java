package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "associated_work_efforts",
    indexes = {
        @Index(name = "idx_awe_associated_record", columnList = "associatedRecordId,associatedRecordType"),
        @Index(name = "idx_awe_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_awe_tenant_effort", columnList = "tenantCode,effortNumber")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AssociatedWorkEffort {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workEffortId;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String effortNumber;

    @Column(nullable = false)
    private Long associatedRecordId;

    @Column(nullable = false, length = 128)
    private String associatedRecordType;

    static AssociatedWorkEffort create(
        WorkEffort workEffort,
        Long associatedRecordId,
        String associatedRecordType
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (associatedRecordId == null) {
            throw new IllegalArgumentException("associatedRecordId is required");
        }
        AssociatedWorkEffort associatedWorkEffort = new AssociatedWorkEffort();
        associatedWorkEffort.workEffortId = workEffort.getId();
        associatedWorkEffort.tenantCode = workEffort.getTenantCode();
        associatedWorkEffort.effortNumber = workEffort.getEffortNumber();
        associatedWorkEffort.associatedRecordId = associatedRecordId;
        associatedWorkEffort.associatedRecordType = normalizeRequired(
            associatedRecordType,
            "associatedRecordType"
        );
        return associatedWorkEffort;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
