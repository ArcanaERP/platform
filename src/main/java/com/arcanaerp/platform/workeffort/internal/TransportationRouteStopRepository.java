package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TransportationRouteStopRepository extends JpaRepository<TransportationRouteStop, UUID> {
    @Query(
        """
        select stop
        from TransportationRouteStop stop
        where (:transportationRouteId is null or stop.transportationRouteId = :transportationRouteId)
          and (:internalIdentifier is null or stop.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or stop.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or stop.externalIdSource = :externalIdSource)
        """
    )
    Page<TransportationRouteStop> findStopsFiltered(
        @Param("transportationRouteId") UUID transportationRouteId,
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
