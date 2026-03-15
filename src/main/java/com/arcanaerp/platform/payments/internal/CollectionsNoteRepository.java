package com.arcanaerp.platform.payments.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsNoteRepository extends JpaRepository<CollectionsNote, UUID> {

    @Query(
        """
        select note
        from CollectionsNote note
        where note.tenantCode = :tenantCode
          and note.invoiceNumber = :invoiceNumber
          and (:notedBy is null or note.notedBy = :notedBy)
          and (:notedAtFrom is null or note.notedAt >= :notedAtFrom)
          and (:notedAtTo is null or note.notedAt <= :notedAtTo)
        """
    )
    Page<CollectionsNote> findHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("notedBy") String notedBy,
        @Param("notedAtFrom") Instant notedAtFrom,
        @Param("notedAtTo") Instant notedAtTo,
        Pageable pageable
    );
}
