package com.arcanaerp.platform.agreements.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "agreement_relationships",
    indexes = {
        @Index(name = "idx_agreement_relationship_type", columnList = "agreement_reln_type_id"),
        @Index(name = "idx_agreement_relationship_from", columnList = "agreementIdFrom"),
        @Index(name = "idx_agreement_relationship_to", columnList = "agreementIdTo"),
        @Index(name = "idx_agreement_relationship_from_role", columnList = "roleTypeIdFrom"),
        @Index(name = "idx_agreement_relationship_to_role", columnList = "roleTypeIdTo"),
        @Index(name = "idx_agreement_relationship_status", columnList = "statusTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AgreementRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agreement_reln_type_id")
    private UUID agreementRelationshipTypeId;

    @Column(length = 512)
    private String description;

    private UUID agreementIdFrom;

    private UUID agreementIdTo;

    private UUID roleTypeIdFrom;

    private UUID roleTypeIdTo;

    private Long statusTypeId;

    private LocalDate fromDate;

    private LocalDate thruDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static AgreementRelationship create(
        UUID agreementRelationshipTypeId,
        String description,
        UUID agreementIdFrom,
        UUID agreementIdTo,
        UUID roleTypeIdFrom,
        UUID roleTypeIdTo,
        Long statusTypeId,
        LocalDate fromDate,
        LocalDate thruDate,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        AgreementRelationship relationship = new AgreementRelationship();
        relationship.agreementRelationshipTypeId = agreementRelationshipTypeId;
        relationship.description = normalizeOptional(description);
        relationship.agreementIdFrom = agreementIdFrom;
        relationship.agreementIdTo = agreementIdTo;
        relationship.roleTypeIdFrom = roleTypeIdFrom;
        relationship.roleTypeIdTo = roleTypeIdTo;
        relationship.statusTypeId = statusTypeId;
        relationship.fromDate = fromDate;
        relationship.thruDate = thruDate;
        relationship.createdAt = createdAt;
        return relationship;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
