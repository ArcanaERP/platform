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
    name = "agreement_items",
    indexes = {
        @Index(name = "idx_agreement_item_agreement", columnList = "agreementId"),
        @Index(name = "idx_agreement_item_type", columnList = "agreementItemTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AgreementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID agreementId;

    private UUID agreementItemTypeId;

    @Column(length = 255)
    private String agreementItemValue;

    @Column(length = 512)
    private String description;

    @Column(length = 1024)
    private String agreementItemRuleString;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static AgreementItem create(
        UUID agreementId,
        UUID agreementItemTypeId,
        String agreementItemValue,
        String description,
        String agreementItemRuleString,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        AgreementItem item = new AgreementItem();
        item.agreementId = agreementId;
        item.agreementItemTypeId = agreementItemTypeId;
        item.agreementItemValue = normalizeOptional(agreementItemValue);
        item.description = normalizeOptional(description);
        item.agreementItemRuleString = normalizeOptional(agreementItemRuleString);
        item.createdAt = createdAt;
        return item;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
