package com.arcanaerp.platform.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SearchCatalogIntegrationTest {

    @Autowired
    private SearchCatalog searchCatalog;

    @Test
    void registersReadsAndSearchesEntries() {
        SearchEntryView created = searchCatalog.registerSearchEntry(
            new RegisterSearchEntryCommand(
                "search01",
                "entry-001",
                "Fraud Hold Order",
                "Order flagged for high-risk review",
                "orders",
                "order",
                "SO-100",
                "/api/orders/SO-100"
            )
        );
        searchCatalog.registerSearchEntry(
            new RegisterSearchEntryCommand(
                "search01",
                "entry-002",
                "Invoice Aging Note",
                "Invoice overdue summary",
                "invoices",
                "invoice",
                "INV-200",
                "/api/invoices/INV-200"
            )
        );

        SearchEntryView loaded = searchCatalog.getSearchEntry("search01", "entry-001");
        var listed = searchCatalog.listSearchEntries("search01", new PageQuery(0, 10), "so-100", "orders");

        assertThat(loaded.entryNumber()).isEqualTo(created.entryNumber());
        assertThat(loaded.tenantCode()).isEqualTo("SEARCH01");
        assertThat(loaded.category()).isEqualTo("ORDERS");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(SearchEntryView::entryNumber).containsExactly("ENTRY-001");
    }

    @Test
    void rejectsDuplicateTenantLocalEntryNumbers() {
        searchCatalog.registerSearchEntry(
            new RegisterSearchEntryCommand(
                "search02",
                "entry-001",
                "Fraud Hold Order",
                "Order flagged for high-risk review",
                "orders",
                "order",
                "SO-100",
                "/api/orders/SO-100"
            )
        );

        assertThatThrownBy(() -> searchCatalog.registerSearchEntry(
            new RegisterSearchEntryCommand(
                "search02",
                "ENTRY-001",
                "Duplicate Search Entry",
                "Duplicate result",
                "orders",
                "order",
                "SO-101",
                "/api/orders/SO-101"
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Search entry already exists for tenant/entryNumber: SEARCH02/ENTRY-001");
    }

    @Test
    void rejectsMissingSearchEntryLookup() {
        assertThatThrownBy(() -> searchCatalog.getSearchEntry("search03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Search entry not found for tenant/entryNumber: SEARCH03/MISSING");
    }
}
