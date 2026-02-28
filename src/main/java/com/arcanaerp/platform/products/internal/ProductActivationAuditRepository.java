package com.arcanaerp.platform.products.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductActivationAuditRepository extends JpaRepository<ProductActivationAudit, UUID> {

    Optional<ProductActivationAudit> findTopByProductIdOrderByChangedAtDesc(UUID productId);

    List<ProductActivationAudit> findByProductIdInOrderByChangedAtDesc(Set<UUID> productIds);

    Page<ProductActivationAudit> findByProductId(UUID productId, Pageable pageable);
}
