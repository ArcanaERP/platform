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
    name = "requirement_types",
    indexes = {
        @Index(name = "idx_req_type_parent", columnList = "parentId"),
        @Index(name = "idx_req_type_internal_identifier", columnList = "internalIdentifier"),
        @Index(name = "idx_req_type_external_reference", columnList = "externalIdentifier,externalIdSource")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RequirementType {

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

    static RequirementType create(
        Long parentId,
        Integer left,
        Integer right,
        String description,
        String comments,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        RequirementType type = new RequirementType();
        type.parentId = parentId;
        type.left = left;
        type.right = right;
        type.description = normalizeOptional(description);
        type.comments = normalizeOptional(comments);
        type.internalIdentifier = normalizeOptional(internalIdentifier);
        type.externalIdentifier = normalizeOptional(externalIdentifier);
        type.externalIdSource = normalizeOptional(externalIdSource);
        type.createdAt = createdAt;
        return type;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
