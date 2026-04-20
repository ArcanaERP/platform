package com.arcanaerp.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class SearchEntryRepositoryTest {

    @Autowired
    private SearchEntryRepository searchEntryRepository;

    @Test
    void filtersEntriesByTenantCategoryAndQueryAcrossTitleSnippetAndTargetIdentifier() {
        searchEntryRepository.save(
            SearchEntry.create(
                "tenant01",
                "entry-001",
                "Fraud Hold Order",
                "Order flagged for high-risk review",
                "orders",
                "order",
                "SO-100",
                "/api/orders/SO-100",
                Instant.parse("2026-04-19T01:00:00Z")
            )
        );
        searchEntryRepository.save(
            SearchEntry.create(
                "tenant01",
                "entry-002",
                "Invoice Aging Note",
                "Invoice overdue summary",
                "invoices",
                "invoice",
                "INV-200",
                "/api/invoices/INV-200",
                Instant.parse("2026-04-19T02:00:00Z")
            )
        );
        searchEntryRepository.save(
            SearchEntry.create(
                "tenant02",
                "entry-003",
                "Other Tenant Order",
                "Different tenant result",
                "orders",
                "order",
                "SO-300",
                "/api/orders/SO-300",
                Instant.parse("2026-04-19T03:00:00Z")
            )
        );

        var page = searchEntryRepository.findFiltered(
            "TENANT01",
            "so-100",
            "ORDERS",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEntryNumber()).isEqualTo("ENTRY-001");
    }
}
