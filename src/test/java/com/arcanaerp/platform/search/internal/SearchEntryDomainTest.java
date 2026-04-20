package com.arcanaerp.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SearchEntryDomainTest {

    @Test
    void normalizesSearchEntryFields() {
        SearchEntry entry = SearchEntry.create(
            "tenant01",
            "entry-001",
            "Orders Search Entry",
            "High-risk order review",
            "orders",
            "order",
            "SO-100",
            "/api/orders/SO-100",
            Instant.parse("2026-04-19T01:00:00Z")
        );

        assertThat(entry.getTenantCode()).isEqualTo("TENANT01");
        assertThat(entry.getEntryNumber()).isEqualTo("ENTRY-001");
        assertThat(entry.getCategory()).isEqualTo("ORDERS");
        assertThat(entry.getTargetType()).isEqualTo("ORDER");
    }

    @Test
    void rejectsMissingTargetUri() {
        assertThatThrownBy(() -> SearchEntry.create(
            "tenant01",
            "entry-001",
            "Orders Search Entry",
            "High-risk order review",
            "orders",
            "order",
            "SO-100",
            "   ",
            Instant.parse("2026-04-19T01:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("targetUri is required");
    }
}
