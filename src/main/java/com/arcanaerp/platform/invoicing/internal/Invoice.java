package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "invoices",
    uniqueConstraints = @UniqueConstraint(name = "uk_invoices_invoice_number", columnNames = "invoiceNumber")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    @Column(nullable = false, length = 64)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant dueAt;

    private Instant issuedAt;

    private Instant voidedAt;

    private Invoice(
        UUID id,
        String tenantCode,
        String invoiceNumber,
        String orderNumber,
        InvoiceStatus status,
        String currencyCode,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant dueAt,
        Instant issuedAt,
        Instant voidedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.invoiceNumber = invoiceNumber;
        this.orderNumber = orderNumber;
        this.status = status;
        this.currencyCode = currencyCode;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.dueAt = dueAt;
        this.issuedAt = issuedAt;
        this.voidedAt = voidedAt;
    }

    static Invoice create(
        String tenantCode,
        String invoiceNumber,
        String orderNumber,
        String currencyCode,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant dueAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        if (dueAt == null) {
            throw new IllegalArgumentException("dueAt is required");
        }
        if (dueAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("dueAt must be on or after createdAt");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("totalAmount must be greater than zero");
        }

        return new Invoice(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            normalizeRequired(orderNumber, "orderNumber").toUpperCase(),
            InvoiceStatus.DRAFT,
            normalizeCurrencyCode(currencyCode),
            totalAmount,
            createdAt,
            dueAt,
            null,
            null
        );
    }

    void transitionTo(InvoiceStatus targetStatus, Instant transitionedAt) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (transitionedAt == null) {
            throw new IllegalArgumentException("transitionedAt is required");
        }
        if (targetStatus == this.status) {
            return;
        }
        if (this.status == InvoiceStatus.VOID) {
            throw new IllegalArgumentException("Invoice status transition not allowed: " + this.status + " -> " + targetStatus);
        }
        if (this.status == InvoiceStatus.DRAFT
            && (targetStatus == InvoiceStatus.ISSUED || targetStatus == InvoiceStatus.VOID)) {
            applyTransition(targetStatus, transitionedAt);
            return;
        }
        if (this.status == InvoiceStatus.ISSUED && targetStatus == InvoiceStatus.VOID) {
            applyTransition(targetStatus, transitionedAt);
            return;
        }
        throw new IllegalArgumentException("Invoice status transition not allowed: " + this.status + " -> " + targetStatus);
    }

    private void applyTransition(InvoiceStatus targetStatus, Instant transitionedAt) {
        this.status = targetStatus;
        if (targetStatus == InvoiceStatus.ISSUED) {
            this.issuedAt = transitionedAt;
            this.voidedAt = null;
            return;
        }
        this.voidedAt = transitionedAt;
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        String normalized = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
        }
        return normalized;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
