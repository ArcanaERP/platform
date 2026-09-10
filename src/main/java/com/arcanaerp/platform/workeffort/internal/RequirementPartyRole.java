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
    name = "requirement_party_roles",
    indexes = {
        @Index(name = "idx_rpr_requirement_party_role", columnList = "requirementId,partyId,roleTypeId"),
        @Index(name = "idx_rpr_party", columnList = "partyId"),
        @Index(name = "idx_rpr_role_type", columnList = "roleTypeId"),
        @Index(name = "idx_rpr_valid_from", columnList = "validFrom"),
        @Index(name = "idx_rpr_valid_to", columnList = "validTo")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RequirementPartyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long requirementId;

    @Column(nullable = false)
    private Long partyId;

    @Column(nullable = false)
    private Long roleTypeId;

    @Column(length = 512)
    private String description;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    private Instant validFrom;

    private Instant validTo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static RequirementPartyRole create(
        Long requirementId,
        Long partyId,
        Long roleTypeId,
        String description,
        String externalIdentifier,
        String externalIdSource,
        Instant validFrom,
        Instant validTo,
        Instant createdAt
    ) {
        if (requirementId == null) {
            throw new IllegalArgumentException("requirementId is required");
        }
        if (partyId == null) {
            throw new IllegalArgumentException("partyId is required");
        }
        if (roleTypeId == null) {
            throw new IllegalArgumentException("roleTypeId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        RequirementPartyRole partyRole = new RequirementPartyRole();
        partyRole.requirementId = requirementId;
        partyRole.partyId = partyId;
        partyRole.roleTypeId = roleTypeId;
        partyRole.description = normalizeOptional(description);
        partyRole.externalIdentifier = normalizeOptional(externalIdentifier);
        partyRole.externalIdSource = normalizeOptional(externalIdSource);
        partyRole.validFrom = validFrom;
        partyRole.validTo = validTo;
        partyRole.createdAt = createdAt;
        return partyRole;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
