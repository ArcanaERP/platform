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
    name = "party_resource_availabilities",
    indexes = {
        @Index(name = "idx_pra_from_date", columnList = "fromDate"),
        @Index(name = "idx_pra_to_date", columnList = "toDate"),
        @Index(name = "idx_pra_type", columnList = "praTypeId"),
        @Index(name = "idx_pra_party", columnList = "partyId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PartyResourceAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Instant fromDate;

    private Instant toDate;

    private Long partyId;

    private Long praTypeId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static PartyResourceAvailability create(
        Instant fromDate,
        Instant toDate,
        Long partyId,
        Long praTypeId,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        PartyResourceAvailability availability = new PartyResourceAvailability();
        availability.fromDate = fromDate;
        availability.toDate = toDate;
        availability.partyId = partyId;
        availability.praTypeId = praTypeId;
        availability.createdAt = createdAt;
        return availability;
    }
}
