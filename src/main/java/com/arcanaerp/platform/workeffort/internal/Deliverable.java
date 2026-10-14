package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "deliverables",
    indexes = {
        @Index(name = "idx_deliverable_internal_identifier", columnList = "internalIdentifier"),
        @Index(name = "idx_deliverable_external_reference", columnList = "externalIdentifier,externalIdSource"),
        @Index(name = "idx_deliverable_record", columnList = "deliverableRecordType,deliverableRecordId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Deliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 512)
    private String description;

    @Lob
    private String comments;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(length = 128)
    private String deliverableRecordType;

    private Long deliverableRecordId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static Deliverable create(
        String description,
        String comments,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        String deliverableRecordType,
        Long deliverableRecordId,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        Deliverable deliverable = new Deliverable();
        deliverable.description = normalizeOptional(description);
        deliverable.comments = normalizeOptional(comments);
        deliverable.internalIdentifier = normalizeOptional(internalIdentifier);
        deliverable.externalIdentifier = normalizeOptional(externalIdentifier);
        deliverable.externalIdSource = normalizeOptional(externalIdSource);
        deliverable.deliverableRecordType = normalizeOptional(deliverableRecordType);
        deliverable.deliverableRecordId = deliverableRecordId;
        deliverable.createdAt = createdAt;
        return deliverable;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
