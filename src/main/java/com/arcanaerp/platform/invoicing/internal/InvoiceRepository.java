package com.arcanaerp.platform.invoicing.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("""
        select invoice
        from Invoice invoice
        where (:tenantCode is null or invoice.tenantCode = :tenantCode)
          and (:status is null or invoice.status = :status)
          and (:currencyCode is null or invoice.currencyCode = :currencyCode)
        """)
    Page<Invoice> findFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("status") com.arcanaerp.platform.invoicing.InvoiceStatus status,
        @Param("currencyCode") String currencyCode,
        Pageable pageable
    );
}
