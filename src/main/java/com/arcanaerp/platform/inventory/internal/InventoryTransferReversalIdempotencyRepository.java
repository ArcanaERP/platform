package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryTransferReversalIdempotencyRepository extends JpaRepository<InventoryTransferReversalIdempotency, UUID> {

    Optional<InventoryTransferReversalIdempotency> findByTransferIdAndIdempotencyKey(UUID transferId, String idempotencyKey);
}
