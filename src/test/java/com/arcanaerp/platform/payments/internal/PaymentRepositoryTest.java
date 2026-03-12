package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void sumsPaymentsByInvoiceNumber() {
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-01",
            "PAY-2000",
            "INV-2000",
            new BigDecimal("10.00"),
            "USD",
            Instant.parse("2026-03-12T00:00:00Z"),
            Instant.parse("2026-03-12T00:01:00Z")
        ));
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-01",
            "PAY-2001",
            "INV-2000",
            new BigDecimal("5.50"),
            "USD",
            Instant.parse("2026-03-12T00:02:00Z"),
            Instant.parse("2026-03-12T00:03:00Z")
        ));

        assertThat(paymentRepository.sumAmountByInvoiceNumber("INV-2000")).isEqualByComparingTo("15.50");
    }

    @Test
    void enforcesUniquePaymentReference() {
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-01",
            "PAY-2002",
            "INV-2002",
            new BigDecimal("10.00"),
            "USD",
            Instant.parse("2026-03-12T00:00:00Z"),
            Instant.parse("2026-03-12T00:01:00Z")
        ));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(Payment.create(
            "TENANT-02",
            "PAY-2002",
            "INV-2003",
            new BigDecimal("12.00"),
            "USD",
            Instant.parse("2026-03-12T00:02:00Z"),
            Instant.parse("2026-03-12T00:03:00Z")
        )))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
