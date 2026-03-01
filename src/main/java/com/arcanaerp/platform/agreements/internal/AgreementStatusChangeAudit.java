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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "agreement_status_change_audits",
    indexes = @Index(name = "idx_asca_agreement_changed", columnList = "agreementId,changedAt")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AgreementStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID agreementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgreementStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgreementStatus currentStatus;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private AgreementStatusChangeAudit(
        UUID id,
        UUID agreementId,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        Instant changedAt
    ) {
        this.id = id;
        this.agreementId = agreementId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.changedAt = changedAt;
    }

    static AgreementStatusChangeAudit create(
        UUID agreementId,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        Instant changedAt
    ) {
        if (agreementId == null) {
            throw new IllegalArgumentException("agreementId is required");
        }
        if (previousStatus == null) {
            throw new IllegalArgumentException("previousStatus is required");
        }
        if (currentStatus == null) {
            throw new IllegalArgumentException("currentStatus is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new AgreementStatusChangeAudit(null, agreementId, previousStatus, currentStatus, changedAt);
    }
}
