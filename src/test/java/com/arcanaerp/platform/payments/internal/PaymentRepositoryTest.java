package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
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

    @Test
    void filtersPaymentsByInvoiceTenantAndPaidAtRange() {
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-01",
            "PAY-2003",
            "INV-2004",
            new BigDecimal("10.00"),
            "USD",
            Instant.parse("2026-03-12T00:00:00Z"),
            Instant.parse("2026-03-12T00:01:00Z")
        ));
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-02",
            "PAY-2004",
            "INV-2005",
            new BigDecimal("12.00"),
            "USD",
            Instant.parse("2026-03-12T00:10:00Z"),
            Instant.parse("2026-03-12T00:11:00Z")
        ));

        var invoiceFiltered = paymentRepository.findFiltered(
            "INV-2004",
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var tenantFiltered = paymentRepository.findFiltered(
            null,
            "TENANT-02",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = paymentRepository.findFiltered(
            null,
            null,
            Instant.parse("2026-03-12T00:05:00Z"),
            Instant.parse("2026-03-12T00:15:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(invoiceFiltered.getTotalElements()).isEqualTo(1);
        assertThat(invoiceFiltered.getContent().get(0).getPaymentReference()).isEqualTo("PAY-2003");
        assertThat(tenantFiltered.getTotalElements()).isEqualTo(1);
        assertThat(tenantFiltered.getContent().get(0).getPaymentReference()).isEqualTo("PAY-2004");
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getPaymentReference()).isEqualTo("PAY-2004");
    }

    @Test
    void summarizesPaymentsByTenantAndCurrency() {
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-03",
            "PAY-2005",
            "INV-2006",
            new BigDecimal("10.00"),
            "USD",
            Instant.parse("2026-03-12T01:00:00Z"),
            Instant.parse("2026-03-12T01:01:00Z")
        ));
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-03",
            "PAY-2006",
            "INV-2007",
            new BigDecimal("5.50"),
            "USD",
            Instant.parse("2026-03-12T01:10:00Z"),
            Instant.parse("2026-03-12T01:11:00Z")
        ));
        paymentRepository.saveAndFlush(Payment.create(
            "TENANT-03",
            "PAY-2007",
            "INV-2007",
            new BigDecimal("2.25"),
            "EUR",
            Instant.parse("2026-03-12T01:20:00Z"),
            Instant.parse("2026-03-12T01:21:00Z")
        ));

        var summary = paymentRepository.summarizeByTenantAndCurrency(
            "TENANT-03",
            "USD",
            Instant.parse("2026-03-12T00:30:00Z"),
            Instant.parse("2026-03-12T01:15:00Z")
        );

        assertThat(summary.getPaymentCount()).isEqualTo(2);
        assertThat(summary.getInvoiceCount()).isEqualTo(2);
        assertThat(summary.getTotalCollected()).isEqualByComparingTo("15.50");
    }
}
