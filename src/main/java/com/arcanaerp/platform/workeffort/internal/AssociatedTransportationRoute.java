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
    name = "associated_transportation_routes",
    indexes = {
        @Index(name = "idx_assoc_route_record", columnList = "associatedRecordId,associatedRecordType"),
        @Index(name = "idx_assoc_route_route", columnList = "transportationRouteId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AssociatedTransportationRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID transportationRouteId;

    private Long associatedRecordId;

    @Column(length = 128)
    private String associatedRecordType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static AssociatedTransportationRoute create(
        TransportationRoute route,
        Long associatedRecordId,
        String associatedRecordType,
        Instant createdAt
    ) {
        if (route == null) {
            throw new IllegalArgumentException("route is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        AssociatedTransportationRoute associated = new AssociatedTransportationRoute();
        associated.transportationRouteId = route.getId();
        associated.associatedRecordId = associatedRecordId;
        associated.associatedRecordType = normalizeOptional(associatedRecordType);
        associated.createdAt = createdAt;
        return associated;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
