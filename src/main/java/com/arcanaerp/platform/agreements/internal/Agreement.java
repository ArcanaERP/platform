package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "agreements",
    uniqueConstraints = @UniqueConstraint(name = "uk_agreements_agreement_number", columnNames = "agreementNumber"),
    indexes = {
        @Index(name = "idx_agreement_type_id", columnList = "agreementTypeId"),
        @Index(name = "idx_agreement_product_id", columnList = "productId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String agreementNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 64)
    private String agreementType;

    @Column(length = 512)
    private String description;

    private UUID agreementTypeId;

    @Column(length = 128)
    private String agreementStatus;

    private Long productId;

    private LocalDate agreementDate;

    private LocalDate fromDate;

    private LocalDate thruDate;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgreementStatus status;

    @Column(nullable = false)
    private Instant effectiveFrom;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant activatedAt;

    private Instant terminatedAt;

    private Agreement(
        UUID id,
        String tenantCode,
        String agreementNumber,
        String name,
        String agreementType,
        String description,
        UUID agreementTypeId,
        String agreementStatus,
        Long productId,
        LocalDate agreementDate,
        LocalDate fromDate,
        LocalDate thruDate,
        String externalIdentifier,
        String externalIdSource,
        AgreementStatus status,
        Instant effectiveFrom,
        Instant createdAt,
        Instant activatedAt,
        Instant terminatedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.agreementNumber = agreementNumber;
        this.name = name;
        this.agreementType = agreementType;
        this.description = description;
        this.agreementTypeId = agreementTypeId;
        this.agreementStatus = agreementStatus;
        this.productId = productId;
        this.agreementDate = agreementDate;
        this.fromDate = fromDate;
        this.thruDate = thruDate;
        this.externalIdentifier = externalIdentifier;
        this.externalIdSource = externalIdSource;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.terminatedAt = terminatedAt;
    }

    static Agreement create(
        String tenantCode,
        String agreementNumber,
        String name,
        String agreementType,
        Instant effectiveFrom,
        Instant createdAt
    ) {
        return create(
            tenantCode,
            agreementNumber,
            name,
            agreementType,
            effectiveFrom,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            createdAt
        );
    }

    static Agreement create(
        String tenantCode,
        String agreementNumber,
        String name,
        String agreementType,
        Instant effectiveFrom,
        String description,
        UUID agreementTypeId,
        String agreementStatus,
        Long productId,
        LocalDate agreementDate,
        LocalDate fromDate,
        LocalDate thruDate,
        String externalIdentifier,
        String externalIdSource,
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
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(agreementNumber, "agreementNumber").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeRequired(agreementType, "agreementType").toUpperCase(),
            normalizeOptional(description),
            agreementTypeId,
            normalizeOptional(agreementStatus),
            productId,
            agreementDate,
            fromDate,
            thruDate,
            normalizeOptional(externalIdentifier),
            normalizeOptional(externalIdSource),
            AgreementStatus.DRAFT,
            effectiveFrom,
            createdAt,
            null,
            null
        );
    }

    void transitionTo(AgreementStatus targetStatus, Instant transitionedAt) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (transitionedAt == null) {
            throw new IllegalArgumentException("transitionedAt is required");
        }
        if (targetStatus == this.status) {
            return;
        }
        boolean allowedFromDraft = this.status == AgreementStatus.DRAFT
            && (targetStatus == AgreementStatus.ACTIVE || targetStatus == AgreementStatus.TERMINATED);
        boolean allowedFromActive = this.status == AgreementStatus.ACTIVE && targetStatus == AgreementStatus.TERMINATED;
        if (!allowedFromDraft && !allowedFromActive) {
            throw new IllegalArgumentException(
                "Agreement status transition not allowed: " + this.status + " -> " + targetStatus
            );
        }
        this.status = targetStatus;
        if (targetStatus == AgreementStatus.ACTIVE) {
            this.activatedAt = transitionedAt;
            this.terminatedAt = null;
        } else {
            this.terminatedAt = transitionedAt;
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
