package com.arcanaerp.platform.invoicing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.math.BigDecimal;
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

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void listsStatusChangesForInvoiceOrderedByChangedAtDesc() {
        UUID invoiceId = UUID.randomUUID();
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.DRAFT,
                InvoiceStatus.VOID,
                "Customer dispute",
                "agent01@invoices.com",
                Instant.parse("2026-03-11T01:00:00Z")
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.DRAFT,
                InvoiceStatus.ISSUED,
                "Ready to bill",
                "agent02@invoices.com",
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
                "Ready to bill",
                "agent01@invoices.com",
                Instant.parse("2026-03-11T01:00:00Z")
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                invoiceId,
                InvoiceStatus.ISSUED,
                InvoiceStatus.VOID,
                "Customer dispute",
                "agent02@invoices.com",
                Instant.parse("2026-03-11T02:00:00Z")
            )
        );

        var currentFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            null,
            InvoiceStatus.ISSUED,
            null,
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
            null,
            PageRequest.of(0, 10)
        );
        var actorFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            null,
            null,
            "agent02@invoices.com",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoiceId,
            null,
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
        assertThat(actorFiltered.getTotalElements()).isEqualTo(1);
        assertThat(actorFiltered.getContent().get(0).getChangedBy()).isEqualTo("agent02@invoices.com");
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void listsGlobalStatusHistoryForActivitySummariesWithTenantAndAuditFilters() {
        Instant createdAt = Instant.parse("2026-04-01T00:00:00Z");
        Invoice firstInvoice = invoiceRepository.save(
            Invoice.create(
                "tenant-a",
                "inv-repo-act-001",
                "so-repo-act-001",
                "USD",
                new BigDecimal("10.00"),
                createdAt,
                createdAt.plusSeconds(86400)
            )
        );
        Invoice secondInvoice = invoiceRepository.save(
            Invoice.create(
                "tenant-b",
                "inv-repo-act-002",
                "so-repo-act-002",
                "USD",
                new BigDecimal("10.00"),
                createdAt,
                createdAt.plusSeconds(86400)
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                firstInvoice.getId(),
                InvoiceStatus.DRAFT,
                InvoiceStatus.ISSUED,
                "Ready to bill",
                "agent01@invoices.com",
                Instant.parse("2026-04-22T10:00:00Z")
            )
        );
        invoiceStatusChangeAuditRepository.save(
            InvoiceStatusChangeAudit.create(
                secondInvoice.getId(),
                InvoiceStatus.ISSUED,
                InvoiceStatus.VOID,
                "Customer dispute",
                "agent02@invoices.com",
                Instant.parse("2026-05-04T12:00:00Z")
            )
        );

        var audits = invoiceStatusChangeAuditRepository.findAllHistoryFiltered(
            "TENANT-B",
            InvoiceStatus.ISSUED,
            InvoiceStatus.VOID,
            "agent02@invoices.com",
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-31T23:59:59Z")
        );

        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getInvoiceId()).isEqualTo(secondInvoice.getId());
        assertThat(audits.get(0).getCurrentStatus()).isEqualTo(InvoiceStatus.VOID);
    }
}
