package com.arcanaerp.platform.invoicing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class InvoiceStatusChangeAuditRepositoryTest {

    @Autowired
    private InvoiceStatusChangeAuditRepository invoiceStatusChangeAuditRepository;

    @Test
    void listsStatusChangesForInvoiceOrderedByChangedAtDesc() {
        UUID invoiceId = UUID.randomUUID();
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.DRAFT,
                InvoiceStatus.VOID,
                Instant.parse("2026-03-11T01:00:00Z")
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.DRAFT,
                InvoiceStatus.ISSUED,
                Instant.parse("2026-03-11T02:00:00Z")
            )
        );

        var page = invoiceStatusChangeAuditRepository.findByInvoiceId(
            invoiceId,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(page.getContent().get(1).getCurrentStatus()).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void filtersStatusHistoryByStatusesAndChangedAtRange() {
        UUID invoiceId = UUID.randomUUID();
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.DRAFT,
                InvoiceStatus.ISSUED,
                Instant.parse("2026-03-11T01:00:00Z")
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.ISSUED,
                InvoiceStatus.VOID,
                Instant.parse("2026-03-11T02:00:00Z")
            )
        );

        var currentFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            null,
            InvoiceStatus.ISSUED,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var previousAndCurrentFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            InvoiceStatus.ISSUED,
            InvoiceStatus.VOID,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            null,
            null,
            Instant.parse("2026-03-11T01:30:00Z"),
            Instant.parse("2026-03-11T02:30:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(currentFiltered.getTotalElements()).isEqualTo(1);
        assertThat(currentFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(previousAndCurrentFiltered.getTotalElements()).isEqualTo(1);
        assertThat(previousAndCurrentFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.VOID);
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.VOID);
    }
}
