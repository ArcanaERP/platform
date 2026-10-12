package com.arcanaerp.platform.workeffort.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PartyResourceAvailabilityRepository extends JpaRepository<PartyResourceAvailability, UUID> {

    @Query(
        """
        select availability
        from PartyResourceAvailability availability
        where (:fromDate is null or availability.fromDate = :fromDate)
          and (:toDate is null or availability.toDate = :toDate)
          and (:partyId is null or availability.partyId = :partyId)
          and (:praTypeId is null or availability.praTypeId = :praTypeId)
        """
    )
    Page<PartyResourceAvailability> findAvailabilitiesFiltered(
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        @Param("partyId") Long partyId,
        @Param("praTypeId") Long praTypeId,
        Pageable pageable
    );
}
