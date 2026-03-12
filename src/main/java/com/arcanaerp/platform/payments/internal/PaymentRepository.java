package com.arcanaerp.platform.payments.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment where payment.invoiceNumber = :invoiceNumber")
    BigDecimal sumAmountByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    @Query(
        """
        select payment
        from Payment payment
        where (:invoiceNumber is null or payment.invoiceNumber = :invoiceNumber)
          and (:tenantCode is null or payment.tenantCode = :tenantCode)
          and (:paidAtFrom is null or payment.paidAt >= :paidAtFrom)
          and (:paidAtTo is null or payment.paidAt <= :paidAtTo)
        """
    )
    Page<Payment> findFiltered(
        @Param("invoiceNumber") String invoiceNumber,
        @Param("tenantCode") String tenantCode,
        @Param("paidAtFrom") Instant paidAtFrom,
        @Param("paidAtTo") Instant paidAtTo,
        Pageable pageable
    );

    @Query(
        value = """
        select
          payment.invoiceNumber as invoiceNumber,
          count(payment) as paymentCount,
          coalesce(sum(payment.amount), 0) as totalCollected
        from Payment payment
        where payment.tenantCode = :tenantCode
          and payment.currencyCode = :currencyCode
          and (:paidAtFrom is null or payment.paidAt >= :paidAtFrom)
          and (:paidAtTo is null or payment.paidAt <= :paidAtTo)
        group by payment.invoiceNumber
        """,
        countQuery = """
        select count(distinct payment.invoiceNumber)
        from Payment payment
        where payment.tenantCode = :tenantCode
          and payment.currencyCode = :currencyCode
          and (:paidAtFrom is null or payment.paidAt >= :paidAtFrom)
          and (:paidAtTo is null or payment.paidAt <= :paidAtTo)
        """
    )
    Page<TenantInvoicePaymentSummaryRow> summarizeInvoicesByTenantAndCurrency(
        @Param("tenantCode") String tenantCode,
        @Param("currencyCode") String currencyCode,
        @Param("paidAtFrom") Instant paidAtFrom,
        @Param("paidAtTo") Instant paidAtTo,
        Pageable pageable
    );

    @Query(
        """
        select
          count(payment) as paymentCount,
          count(distinct payment.invoiceNumber) as invoiceCount,
          coalesce(sum(payment.amount), 0) as totalCollected
        from Payment payment
        where payment.tenantCode = :tenantCode
          and payment.currencyCode = :currencyCode
          and (:paidAtFrom is null or payment.paidAt >= :paidAtFrom)
          and (:paidAtTo is null or payment.paidAt <= :paidAtTo)
        """
    )
    TenantPaymentSummaryRow summarizeByTenantAndCurrency(
        @Param("tenantCode") String tenantCode,
        @Param("currencyCode") String currencyCode,
        @Param("paidAtFrom") Instant paidAtFrom,
        @Param("paidAtTo") Instant paidAtTo
    );
}
