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
    name = "transportation_route_segments",
    indexes = {
        @Index(name = "idx_route_segment_route", columnList = "transportationRouteId"),
        @Index(name = "idx_route_segment_from_stop", columnList = "fromTransportationRouteStopId"),
        @Index(name = "idx_route_segment_to_stop", columnList = "toTransportationRouteStopId"),
        @Index(name = "idx_route_segment_external_reference", columnList = "externalIdentifier,externalIdSource")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TransportationRouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID transportationRouteId;

    private UUID fromTransportationRouteStopId;

    private UUID toTransportationRouteStopId;

    @Column(length = 128)
    private String internalIdentifier;

    @Column(length = 512)
    private String description;

    @Lob
    private String comments;

    @Column(length = 128)
    private String externalIdentifier;

    @Column(length = 128)
    private String externalIdSource;

    private Integer sequence;

    private Instant estimatedStart;

    @Column(name = "estmated_arrival")
    private Instant estimatedArrival;

    private Instant actualStart;

    private Instant actualArrival;

    private Integer startMileage;

    @Column(name = "end_milage")
    private Integer endMileage;

    private Integer fuelUsed;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static TransportationRouteSegment create(
        UUID transportationRouteId,
        UUID fromTransportationRouteStopId,
        UUID toTransportationRouteStopId,
        String internalIdentifier,
        String description,
        String comments,
        String externalIdentifier,
        String externalIdSource,
        Integer sequence,
        Instant estimatedStart,
        Instant estimatedArrival,
        Instant actualStart,
        Instant actualArrival,
        Integer startMileage,
        Integer endMileage,
        Integer fuelUsed,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        TransportationRouteSegment segment = new TransportationRouteSegment();
        segment.transportationRouteId = transportationRouteId;
        segment.fromTransportationRouteStopId = fromTransportationRouteStopId;
        segment.toTransportationRouteStopId = toTransportationRouteStopId;
        segment.internalIdentifier = normalizeOptional(internalIdentifier);
        segment.description = normalizeOptional(description);
        segment.comments = normalizeOptional(comments);
        segment.externalIdentifier = normalizeOptional(externalIdentifier);
        segment.externalIdSource = normalizeOptional(externalIdSource);
        segment.sequence = sequence;
        segment.estimatedStart = estimatedStart;
        segment.estimatedArrival = estimatedArrival;
        segment.actualStart = actualStart;
        segment.actualArrival = actualArrival;
        segment.startMileage = startMileage;
        segment.endMileage = endMileage;
        segment.fuelUsed = fuelUsed;
        segment.createdAt = createdAt;
        return segment;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
