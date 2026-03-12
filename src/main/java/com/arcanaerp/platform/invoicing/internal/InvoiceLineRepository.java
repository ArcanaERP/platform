package com.arcanaerp.platform.invoicing.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

    List<InvoiceLine> findByInvoiceIdOrderByLineNoAsc(UUID invoiceId);

    List<InvoiceLine> findByInvoiceIdInOrderByInvoiceIdAscLineNoAsc(Collection<UUID> invoiceIds);
}
