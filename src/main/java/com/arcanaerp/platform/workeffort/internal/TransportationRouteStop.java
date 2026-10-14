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
    name = "transportation_route_stops",
    indexes = {
        @Index(name = "idx_route_stop_route", columnList = "transportationRouteId"),
        @Index(name = "idx_route_stop_internal_identifier", columnList = "internalIdentifier"),
        @Index(name = "idx_route_stop_external_reference", columnList = "externalIdentifier,externalIdSource")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TransportationRouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID transportationRouteId;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(length = 512)
    private String description;

    private Long postalAddressId;

    @Column(length = 256)
    private String geoloc;

    private Integer sequence;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static TransportationRouteStop create(
        UUID transportationRouteId,
        String internalIdentifier,
        String description,
        Long postalAddressId,
        String geoloc,
        Integer sequence,
        String externalIdentifier,
        String externalIdSource,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        TransportationRouteStop stop = new TransportationRouteStop();
        stop.transportationRouteId = transportationRouteId;
        stop.internalIdentifier = normalizeOptional(internalIdentifier);
        stop.description = normalizeOptional(description);
        stop.postalAddressId = postalAddressId;
        stop.geoloc = normalizeOptional(geoloc);
        stop.sequence = sequence;
        stop.externalIdentifier = normalizeOptional(externalIdentifier);
        stop.externalIdSource = normalizeOptional(externalIdSource);
        stop.createdAt = createdAt;
        return stop;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
