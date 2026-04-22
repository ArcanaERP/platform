package com.arcanaerp.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommerceCatalogIntegrationTest {

    @Autowired
    private CommerceCatalog commerceCatalog;

    @Test
    void createsReadsAndListsStorefronts() {
        StorefrontView created = commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce01", "b2c-main", "B2C Main", "USD", "en-US", true)
        );
        commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce01", "wholesale", "Wholesale", "USD", "en-US", false)
        );

        StorefrontView loaded = commerceCatalog.getStorefront("commerce01", "b2c-main");
        var listed = commerceCatalog.listStorefronts("commerce01", new PageQuery(0, 10), true);

        assertThat(loaded.storefrontCode()).isEqualTo(created.storefrontCode());
        assertThat(loaded.tenantCode()).isEqualTo("COMMERCE01");
        assertThat(loaded.currencyCode()).isEqualTo("USD");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(StorefrontView::storefrontCode).containsExactly("B2C-MAIN");
    }

    @Test
    void rejectsDuplicateTenantLocalStorefrontCodes() {
        commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce02", "b2c-main", "B2C Main", "USD", "en-US", true)
        );

        assertThatThrownBy(() -> commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce02", "B2C-MAIN", "Duplicate", "USD", "en-US", true)
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Storefront already exists for tenant/code: COMMERCE02/B2C-MAIN");
    }

    @Test
    void rejectsMissingStorefrontLookup() {
        assertThatThrownBy(() -> commerceCatalog.getStorefront("commerce03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Storefront not found for tenant/code: COMMERCE03/MISSING");
    }
}
