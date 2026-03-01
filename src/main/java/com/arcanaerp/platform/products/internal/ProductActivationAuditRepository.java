package com.arcanaerp.platform.products.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductActivationAuditRepository extends JpaRepository<ProductActivationAudit, UUID> {

    Optional<ProductActivationAudit> findTopByProductIdOrderByChangedAtDesc(UUID productId);

    List<ProductActivationAudit> findByProductIdInOrderByChangedAtDesc(Set<UUID> productIds);

    Page<ProductActivationAudit> findByProductId(UUID productId, Pageable pageable);

    Page<ProductActivationAudit> findByProductIdAndTenantCode(UUID productId, String tenantCode, Pageable pageable);

    Page<ProductActivationAudit> findByProductIdAndChangedBy(UUID productId, String changedBy, Pageable pageable);

    Page<ProductActivationAudit> findByProductIdAndTenantCodeAndChangedBy(
        UUID productId,
        String tenantCode,
        String changedBy,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from ProductActivationAudit audit
        where audit.productId = :productId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:currentActive is null or audit.currentActive = :currentActive)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<ProductActivationAudit> findHistoryFiltered(
        @Param("productId") UUID productId,
        @Param("tenantCode") String tenantCode,
        @Param("changedBy") String changedBy,
        @Param("currentActive") Boolean currentActive,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
