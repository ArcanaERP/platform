package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
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
    name = "invoice_status_change_audits",
    indexes = {
        @Index(name = "idx_isca_invoice_changed", columnList = "invoiceId,changedAt"),
        @Index(name = "idx_isca_invoice_current_changed", columnList = "invoiceId,currentStatus,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InvoiceStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus currentStatus;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private InvoiceStatusChangeAudit(
        UUID id,
        UUID invoiceId,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        Instant changedAt
    ) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.changedAt = changedAt;
    }

    static InvoiceStatusChangeAudit create(
        UUID invoiceId,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        Instant changedAt
    ) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId is required");
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
        return new InvoiceStatusChangeAudit(null, invoiceId, previousStatus, currentStatus, changedAt);
    }
}
