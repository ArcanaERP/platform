package com.arcanaerp.platform.invoicing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InvoiceLineRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineRepository invoiceLineRepository;

    @Test
    void findsInvoiceLinesByInvoiceIdInLineSequence() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        Invoice invoice = invoiceRepository.saveAndFlush(Invoice.create(
            "TENANT-01",
            "INV-3000",
            "SO-3000",
            "USD",
            new BigDecimal("25.00"),
            now,
            now.plusSeconds(86400)
        ));
        invoiceLineRepository.saveAndFlush(InvoiceLine.create(
            invoice.getId(),
            1,
            "arc-3000",
            new BigDecimal("2"),
            new BigDecimal("5.00"),
            now
        ));
        invoiceLineRepository.saveAndFlush(InvoiceLine.create(
            invoice.getId(),
            2,
            "arc-3001",
            new BigDecimal("3"),
            new BigDecimal("5.00"),
            now
        ));

        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdOrderByLineNoAsc(invoice.getId());

        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(InvoiceLine::getLineNo).containsExactly(1, 2);
        assertThat(lines).extracting(InvoiceLine::getProductSku).containsExactly("ARC-3000", "ARC-3001");
    }
}
