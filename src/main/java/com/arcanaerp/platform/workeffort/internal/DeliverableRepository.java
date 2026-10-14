package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DeliverableRepository extends JpaRepository<Deliverable, UUID> {

    @Query(
        """
        select deliverable
        from Deliverable deliverable
        where (:internalIdentifier is null or deliverable.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or deliverable.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or deliverable.externalIdSource = :externalIdSource)
          and (:deliverableRecordType is null or deliverable.deliverableRecordType = :deliverableRecordType)
          and (:deliverableRecordId is null or deliverable.deliverableRecordId = :deliverableRecordId)
        """
    )
    Page<Deliverable> findDeliverablesFiltered(
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        @Param("deliverableRecordType") String deliverableRecordType,
        @Param("deliverableRecordId") Long deliverableRecordId,
        Pageable pageable
    );
}
