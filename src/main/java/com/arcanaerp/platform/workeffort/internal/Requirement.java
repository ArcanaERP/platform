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
    name = "requirements",
    indexes = {
        @Index(name = "idx_req_parent", columnList = "parentId"),
        @Index(name = "idx_req_requirement_type", columnList = "requirementTypeId"),
        @Index(name = "idx_req_record", columnList = "requirementRecordId,requirementRecordType"),
        @Index(name = "idx_req_fixed_asset", columnList = "fixedAssetId"),
        @Index(name = "idx_req_product", columnList = "productId"),
        @Index(name = "idx_req_deliverable", columnList = "deliverableId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long parentId;

    @Column(name = "lft")
    private Integer left;

    @Column(name = "rgt")
    private Integer right;

    @Column(length = 512)
    private String description;

    @Column(name = "type", length = 128)
    private String requirementSubtype;

    private Integer projectedCompletionTime;

    private Long estimatedBudgetMoneyId;

    private Long requirementTypeId;

    private Long requirementRecordId;

    @Column(length = 128)
    private String requirementRecordType;

    private Long fixedAssetId;

    private Long productId;

    private Long deliverableId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static Requirement create(
        Long parentId,
        Integer left,
        Integer right,
        String description,
        String requirementSubtype,
        Integer projectedCompletionTime,
        Long estimatedBudgetMoneyId,
        Long requirementTypeId,
        Long requirementRecordId,
        String requirementRecordType,
        Long fixedAssetId,
        Long productId,
        Long deliverableId,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        Requirement requirement = new Requirement();
        requirement.parentId = parentId;
        requirement.left = left;
        requirement.right = right;
        requirement.description = normalizeOptional(description);
        requirement.requirementSubtype = normalizeOptional(requirementSubtype);
        requirement.projectedCompletionTime = projectedCompletionTime;
        requirement.estimatedBudgetMoneyId = estimatedBudgetMoneyId;
        requirement.requirementTypeId = requirementTypeId;
        requirement.requirementRecordId = requirementRecordId;
        requirement.requirementRecordType = normalizeOptional(requirementRecordType);
        requirement.fixedAssetId = fixedAssetId;
        requirement.productId = productId;
        requirement.deliverableId = deliverableId;
        requirement.createdAt = createdAt;
        return requirement;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
