package com.arcanaerp.platform.workeffort.internal;

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
    name = "party_resource_availability_types",
    indexes = {
        @Index(name = "idx_prat_internal_identifier", columnList = "internalIdentifier"),
        @Index(name = "idx_prat_description", columnList = "description")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PartyResourceAvailabilityType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 512)
    private String description;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static PartyResourceAvailabilityType create(
        String description,
        String internalIdentifier,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        PartyResourceAvailabilityType type = new PartyResourceAvailabilityType();
        type.description = normalizeOptional(description);
        type.internalIdentifier = normalizeOptional(internalIdentifier);
        type.createdAt = createdAt;
        return type;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
