package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "work_effort_type_associations",
    indexes = {
        @Index(name = "idx_weta_assoc_type", columnList = "workEffortTypeAssociationTypeId"),
        @Index(name = "idx_weta_from_type", columnList = "fromWorkEffortTypeId"),
        @Index(name = "idx_weta_to_type", columnList = "toWorkEffortTypeId"),
        @Index(name = "idx_weta_internal_identifier", columnList = "internalIdentifier"),
        @Index(name = "idx_weta_external_reference", columnList = "externalIdentifier,externalIdSource")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortTypeAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long workEffortTypeAssociationTypeId;

    @Column(nullable = false)
    private Long fromWorkEffortTypeId;

    @Column(nullable = false)
    private Long toWorkEffortTypeId;

    @Column(length = 512)
    private String description;

    @Lob
    private String comments;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortTypeAssociation create(
        Long workEffortTypeAssociationTypeId,
        Long fromWorkEffortTypeId,
        Long toWorkEffortTypeId,
        String description,
        String comments,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (workEffortTypeAssociationTypeId == null) {
            throw new IllegalArgumentException("workEffortTypeAssociationTypeId is required");
        }
        if (fromWorkEffortTypeId == null) {
            throw new IllegalArgumentException("fromWorkEffortTypeId is required");
        }
        if (toWorkEffortTypeId == null) {
            throw new IllegalArgumentException("toWorkEffortTypeId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortTypeAssociation association = new WorkEffortTypeAssociation();
        association.workEffortTypeAssociationTypeId = workEffortTypeAssociationTypeId;
        association.fromWorkEffortTypeId = fromWorkEffortTypeId;
        association.toWorkEffortTypeId = toWorkEffortTypeId;
        association.description = normalizeOptional(description);
        association.comments = normalizeOptional(comments);
        association.internalIdentifier = normalizeOptional(internalIdentifier);
        association.externalIdentifier = normalizeOptional(externalIdentifier);
        association.externalIdSource = normalizeOptional(externalIdSource);
        association.createdAt = createdAt;
        return association;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
