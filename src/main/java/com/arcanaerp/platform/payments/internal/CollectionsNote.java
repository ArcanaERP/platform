package com.arcanaerp.platform.payments.internal;

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
    name = "collections_notes",
    indexes = {
        @Index(name = "idx_cn_tenant_invoice_noted", columnList = "tenantCode,invoiceNumber,notedAt"),
        @Index(name = "idx_cn_tenant_noted_by", columnList = "tenantCode,notedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CollectionsNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    @Column(nullable = false, length = 1000)
    private String note;

    @Column(nullable = false, length = 128)
    private String notedBy;

    @Column(nullable = false, updatable = false)
    private Instant notedAt;

    private CollectionsNote(
        UUID id,
        String tenantCode,
        String invoiceNumber,
        String note,
        String notedBy,
        Instant notedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.invoiceNumber = invoiceNumber;
        this.note = note;
        this.notedBy = notedBy;
        this.notedAt = notedAt;
    }

    static CollectionsNote create(
        String tenantCode,
        String invoiceNumber,
        String note,
        String notedBy,
        Instant notedAt
    ) {
        if (notedAt == null) {
            throw new IllegalArgumentException("notedAt is required");
        }
        return new CollectionsNote(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            normalizeNote(note),
            normalizeActorEmail(notedBy, "notedBy"),
            notedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeNote(String value) {
        String normalized = normalizeRequired(value, "note");
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("note must be at most 1000 characters");
        }
        return normalized;
    }

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
