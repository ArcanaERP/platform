package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PartyResourceAvailabilityTypeRepository extends JpaRepository<PartyResourceAvailabilityType, UUID> {

    @Query(
        """
        select type
        from PartyResourceAvailabilityType type
        where (:description is null or type.description = :description)
          and (:internalIdentifier is null or type.internalIdentifier = :internalIdentifier)
        """
    )
    Page<PartyResourceAvailabilityType> findTypesFiltered(
        @Param("description") String description,
        @Param("internalIdentifier") String internalIdentifier,
        Pageable pageable
    );
}
