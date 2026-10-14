package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssociatedTransportationRouteRepository extends JpaRepository<AssociatedTransportationRoute, UUID> {

    @Query(
        """
        select associated
        from AssociatedTransportationRoute associated
        where (:transportationRouteId is null or associated.transportationRouteId = :transportationRouteId)
          and (:associatedRecordId is null or associated.associatedRecordId = :associatedRecordId)
          and (:associatedRecordType is null or associated.associatedRecordType = :associatedRecordType)
        """
    )
    Page<AssociatedTransportationRoute> findAssociatedRoutesFiltered(
        @Param("transportationRouteId") UUID transportationRouteId,
        @Param("associatedRecordId") Long associatedRecordId,
        @Param("associatedRecordType") String associatedRecordType,
        Pageable pageable
    );
}
