package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TransportationRouteRepository extends JpaRepository<TransportationRoute, UUID> {

    @Query(
        """
        select route
        from TransportationRoute route
        where (:internalIdentifier is null or route.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or route.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or route.externalIdSource = :externalIdSource)
        """
    )
    Page<TransportationRoute> findRoutesFiltered(
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
