package com.arcanaerp.platform.payments.internal;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment where payment.invoiceNumber = :invoiceNumber")
    BigDecimal sumAmountByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);
}
