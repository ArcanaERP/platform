package com.arcanaerp.platform.payments.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CollectionsAssignmentRepository extends JpaRepository<CollectionsAssignment, UUID> {

    Optional<CollectionsAssignment> findByInvoiceNumber(String invoiceNumber);

    List<CollectionsAssignment> findByInvoiceNumberIn(Collection<String> invoiceNumbers);
}
