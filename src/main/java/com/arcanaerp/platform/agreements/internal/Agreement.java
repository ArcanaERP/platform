package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "agreements",
    uniqueConstraints = @UniqueConstraint(name = "uk_agreements_agreement_number", columnNames = "agreementNumber")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String agreementNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 64)
    private String agreementType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgreementStatus status;

    @Column(nullable = false)
    private Instant effectiveFrom;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Agreement(
        UUID id,
        String agreementNumber,
        String name,
        String agreementType,
        AgreementStatus status,
        Instant effectiveFrom,
        Instant createdAt
    ) {
        this.id = id;
        this.agreementNumber = agreementNumber;
        this.name = name;
        this.agreementType = agreementType;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    static Agreement create(
        String agreementNumber,
        String name,
        String agreementType,
        Instant effectiveFrom,
        Instant createdAt
    ) {
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }

        return new Agreement(
            null,
            normalizeRequired(agreementNumber, "agreementNumber").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeRequired(agreementType, "agreementType").toUpperCase(),
            AgreementStatus.DRAFT,
            effectiveFrom,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
