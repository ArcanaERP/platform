package com.arcanaerp.platform.payments.internal;

import com.arcanaerp.platform.payments.CollectionsNoteCategory;
import com.arcanaerp.platform.payments.CollectionsNoteOutcome;
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
          and (:category is null or note.category = :category)
          and (:outcome is null or note.outcome = :outcome)
          and (:notedAtFrom is null or note.notedAt >= :notedAtFrom)
          and (:notedAtTo is null or note.notedAt <= :notedAtTo)
        """
    )
    Page<CollectionsNote> findHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("notedBy") String notedBy,
        @Param("category") CollectionsNoteCategory category,
        @Param("outcome") CollectionsNoteOutcome outcome,
        @Param("notedAtFrom") Instant notedAtFrom,
        @Param("notedAtTo") Instant notedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select note
        from CollectionsNote note
        where note.tenantCode = :tenantCode
          and (:invoiceNumber is null or note.invoiceNumber = :invoiceNumber)
          and (:assignedTo is null or exists (
              select assignment.id
              from CollectionsAssignment assignment
              where assignment.tenantCode = note.tenantCode
                and assignment.invoiceNumber = note.invoiceNumber
                and assignment.assignedTo = :assignedTo
          ))
          and (:notedBy is null or note.notedBy = :notedBy)
          and (:category is null or note.category = :category)
          and (:outcome is null or note.outcome = :outcome)
          and (:notedAtFrom is null or note.notedAt >= :notedAtFrom)
          and (:notedAtTo is null or note.notedAt <= :notedAtTo)
        """
    )
    Page<CollectionsNote> findTenantHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("assignedTo") String assignedTo,
        @Param("notedBy") String notedBy,
        @Param("category") CollectionsNoteCategory category,
        @Param("outcome") CollectionsNoteOutcome outcome,
        @Param("notedAtFrom") Instant notedAtFrom,
        @Param("notedAtTo") Instant notedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select note
        from CollectionsNote note
        where note.tenantCode = :tenantCode
          and (:assignedTo is null or exists (
              select assignment.id
              from CollectionsAssignment assignment
              where assignment.tenantCode = note.tenantCode
                and assignment.invoiceNumber = note.invoiceNumber
                and assignment.assignedTo = :assignedTo
          ))
          and (:notedBy is null or note.notedBy = :notedBy)
          and (:category is null or note.category = :category)
          and (:notedAtFrom is null or note.notedAt >= :notedAtFrom)
          and (:notedAtTo is null or note.notedAt <= :notedAtTo)
        order by note.notedAt desc, note.id desc
        """
    )
    java.util.List<CollectionsNote> findTenantHistoryForOutcomeSummary(
        @Param("tenantCode") String tenantCode,
        @Param("assignedTo") String assignedTo,
        @Param("notedBy") String notedBy,
        @Param("category") CollectionsNoteCategory category,
        @Param("notedAtFrom") Instant notedAtFrom,
        @Param("notedAtTo") Instant notedAtTo
    );

    @Query(
        """
        select note
        from CollectionsNote note
        where note.tenantCode = :tenantCode
          and (:assignedTo is null or exists (
              select assignment.id
              from CollectionsAssignment assignment
              where assignment.tenantCode = note.tenantCode
                and assignment.invoiceNumber = note.invoiceNumber
                and assignment.assignedTo = :assignedTo
          ))
          and (:notedBy is null or note.notedBy = :notedBy)
          and (:outcome is null or note.outcome = :outcome)
          and (:notedAtFrom is null or note.notedAt >= :notedAtFrom)
          and (:notedAtTo is null or note.notedAt <= :notedAtTo)
        order by note.notedAt desc, note.id desc
        """
    )
    java.util.List<CollectionsNote> findTenantHistoryForCategorySummary(
        @Param("tenantCode") String tenantCode,
        @Param("assignedTo") String assignedTo,
        @Param("notedBy") String notedBy,
        @Param("outcome") CollectionsNoteOutcome outcome,
        @Param("notedAtFrom") Instant notedAtFrom,
        @Param("notedAtTo") Instant notedAtTo
    );
}
