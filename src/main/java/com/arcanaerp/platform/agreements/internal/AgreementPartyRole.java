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
    name = "agreement_party_roles",
    indexes = {
        @Index(name = "idx_agreement_party_role_agreement", columnList = "agreementId"),
        @Index(name = "idx_agreement_party_role_party", columnList = "partyId"),
        @Index(name = "idx_agreement_party_role_role_type", columnList = "roleTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AgreementPartyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 512)
    private String description;

    private UUID agreementId;

    private Long partyId;

    private Long roleTypeId;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static AgreementPartyRole create(
        String description,
        UUID agreementId,
        Long partyId,
        Long roleTypeId,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        AgreementPartyRole partyRole = new AgreementPartyRole();
        partyRole.description = normalizeOptional(description);
        partyRole.agreementId = agreementId;
        partyRole.partyId = partyId;
        partyRole.roleTypeId = roleTypeId;
        partyRole.externalIdentifier = normalizeOptional(externalIdentifier);
        partyRole.externalIdSource = normalizeOptional(externalIdSource);
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
