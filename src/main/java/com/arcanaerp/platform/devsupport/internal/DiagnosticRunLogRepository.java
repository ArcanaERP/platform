package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DiagnosticRunLogRepository extends JpaRepository<DiagnosticRunLog, UUID> {

    Optional<DiagnosticRunLog> findByTenantCodeAndRunNumber(String tenantCode, String runNumber);

    @Query(
        """
        select diagnosticRunLog
        from DiagnosticRunLog diagnosticRunLog
        where diagnosticRunLog.tenantCode = :tenantCode
          and (:status is null or diagnosticRunLog.status = :status)
          and (:startedAtFrom is null or diagnosticRunLog.startedAt >= :startedAtFrom)
          and (:startedAtTo is null or diagnosticRunLog.startedAt <= :startedAtTo)
        """
    )
    Page<DiagnosticRunLog> findFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("status") DiagnosticRunStatus status,
        @Param("startedAtFrom") Instant startedAtFrom,
        @Param("startedAtTo") Instant startedAtTo,
        Pageable pageable
    );
}
