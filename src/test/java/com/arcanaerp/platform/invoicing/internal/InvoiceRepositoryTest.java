package com.arcanaerp.platform.invoicing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void findsInvoiceByInvoiceNumber() {
        invoiceRepository.saveAndFlush(Invoice.create(
            "TENANT-01",
            "inv-2000",
            "so-2000",
            "USD",
            new BigDecimal("25.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z")
        ));

        Invoice invoice = invoiceRepository.findByInvoiceNumber("INV-2000").orElseThrow();

        assertThat(invoice.getOrderNumber()).isEqualTo("SO-2000");
    }

    @Test
    void enforcesUniqueInvoiceNumber() {
        invoiceRepository.saveAndFlush(Invoice.create(
            "TENANT-01",
            "INV-2001",
            "SO-2001",
            "USD",
            new BigDecimal("25.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-15T00:00:00Z")
        ));

        assertThatThrownBy(() -> invoiceRepository.saveAndFlush(Invoice.create(
            "TENANT-02",
            "INV-2001",
            "SO-2002",
            "USD",
            new BigDecimal("30.00"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-16T00:00:00Z")
        )))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
