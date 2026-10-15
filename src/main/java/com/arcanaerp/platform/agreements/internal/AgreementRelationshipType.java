package com.arcanaerp.platform.agreements.internal;

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
    name = "agreement_reln_types",
    indexes = {
        @Index(name = "idx_agreement_reln_type_parent", columnList = "parentId"),
        @Index(name = "idx_agreement_reln_type_valid_from_role", columnList = "validFromRoleTypeId"),
        @Index(name = "idx_agreement_reln_type_valid_to_role", columnList = "validToRoleTypeId"),
        @Index(name = "idx_agreement_reln_type_internal_identifier", columnList = "internalIdentifier"),
        @Index(
            name = "idx_agreement_reln_type_external_reference",
            columnList = "externalIdentifier,externalIdSource"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AgreementRelationshipType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long parentId;

    @Column(name = "lft")
    private Integer left;

    @Column(name = "rgt")
    private Integer right;

    private UUID validFromRoleTypeId;

    private UUID validToRoleTypeId;

    @Column(length = 255)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static AgreementRelationshipType create(
        Long parentId,
        Integer left,
        Integer right,
        UUID validFromRoleTypeId,
        UUID validToRoleTypeId,
        String name,
        String description,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        AgreementRelationshipType type = new AgreementRelationshipType();
        type.parentId = parentId;
        type.left = left;
        type.right = right;
        type.validFromRoleTypeId = validFromRoleTypeId;
        type.validToRoleTypeId = validToRoleTypeId;
        type.name = normalizeOptional(name);
        type.description = normalizeOptional(description);
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
