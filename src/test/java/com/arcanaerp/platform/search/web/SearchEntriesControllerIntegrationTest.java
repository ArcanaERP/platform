package com.arcanaerp.platform.search.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SearchEntriesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndSearchesEntries() throws Exception {
        SearchWebIntegrationTestSupport.createSearchEntry(
            mockMvc,
            "searchweb01",
            "entry-001",
            "Fraud Hold Order",
            "Order flagged for high-risk review",
            "orders",
            "order",
            "SO-100",
            "/api/orders/SO-100"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("SEARCHWEB01"))
            .andExpect(jsonPath("$.entryNumber").value("ENTRY-001"))
            .andExpect(jsonPath("$.category").value("ORDERS"))
            .andExpect(jsonPath("$.targetType").value("ORDER"));

        SearchWebIntegrationTestSupport.createSearchEntry(
            mockMvc,
            "searchweb01",
            "entry-002",
            "Invoice Aging Note",
            "Invoice overdue summary",
            "invoices",
            "invoice",
            "INV-200",
            "/api/invoices/INV-200"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(SearchWebIntegrationTestSupport.getSearchEntryRequest("searchweb01", "entry-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entryNumber").value("ENTRY-001"))
            .andExpect(jsonPath("$.title").value("Fraud Hold Order"));

        mockMvc.perform(
            SearchWebIntegrationTestSupport.listSearchEntriesRequest(
                "searchweb01",
                0,
                10,
                "query", "so-100",
                "category", "orders"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.entryNumber=='ENTRY-001')].title", hasItem("Fraud Hold Order")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        SearchWebIntegrationTestSupport.createSearchEntry(
            mockMvc,
            "searchweb02",
            "entry-001",
            "Credit Hold Order",
            "Order flagged for credit review",
            "orders",
            "order",
            "SO-200",
            "/api/orders/SO-200"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(SearchWebIntegrationTestSupport.listSearchEntriesRequest("searchweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.entryNumber=='ENTRY-001')].title", hasItem("Credit Hold Order")));
    }

    @Test
    void rejectsDuplicateTenantLocalEntryNumbers() throws Exception {
        SearchWebIntegrationTestSupport.createSearchEntry(
            mockMvc,
            "searchweb03",
            "entry-001",
            "Fraud Hold Order",
            "Order flagged for high-risk review",
            "orders",
            "order",
            "SO-100",
            "/api/orders/SO-100"
        )
            .andExpect(status().isCreated());

        SearchWebIntegrationTestSupport.createSearchEntry(
            mockMvc,
            "searchweb03",
            "ENTRY-001",
            "Duplicate Search Entry",
            "Duplicate result",
            "orders",
            "order",
            "SO-101",
            "/api/orders/SO-101"
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Search entry already exists for tenant/entryNumber: SEARCHWEB03/ENTRY-001"))
            .andExpect(jsonPath("$.path").value("/api/search/entries"));
    }

    @Test
    void returnsNotFoundForMissingSearchEntry() throws Exception {
        mockMvc.perform(SearchWebIntegrationTestSupport.getSearchEntryRequest("searchweb04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Search entry not found for tenant/entryNumber: SEARCHWEB04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/search/entries/missing"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(
            SearchWebIntegrationTestSupport.listSearchEntriesRequest("searchweb05", 0, 10, "query", "   ")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("query query parameter must not be blank"));

        mockMvc.perform(
            SearchWebIntegrationTestSupport.listSearchEntriesRequest("searchweb05", 0, 10, "category", "   ")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("category query parameter must not be blank"));

        mockMvc.perform(SearchWebIntegrationTestSupport.listSearchEntriesRequest("searchweb05", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
