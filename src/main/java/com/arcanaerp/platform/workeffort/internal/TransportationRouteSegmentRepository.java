package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TransportationRouteSegmentRepository extends JpaRepository<TransportationRouteSegment, UUID> {
    @Query(
        """
        select segment
        from TransportationRouteSegment segment
        where (:transportationRouteId is null or segment.transportationRouteId = :transportationRouteId)
          and (:fromTransportationRouteStopId is null or segment.fromTransportationRouteStopId = :fromTransportationRouteStopId)
          and (:toTransportationRouteStopId is null or segment.toTransportationRouteStopId = :toTransportationRouteStopId)
          and (:internalIdentifier is null or segment.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or segment.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or segment.externalIdSource = :externalIdSource)
        """
    )
    Page<TransportationRouteSegment> findSegmentsFiltered(
        @Param("transportationRouteId") UUID transportationRouteId,
        @Param("fromTransportationRouteStopId") UUID fromTransportationRouteStopId,
        @Param("toTransportationRouteStopId") UUID toTransportationRouteStopId,
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
