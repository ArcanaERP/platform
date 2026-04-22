package com.arcanaerp.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.products.ProductCatalog;
import com.arcanaerp.platform.products.ProductOrderability;
import com.arcanaerp.platform.products.RegisterProductCommand;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommerceCatalogIntegrationTest {

    @Autowired
    private CommerceCatalog commerceCatalog;

    @Autowired
    private ProductCatalog productCatalog;

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

    @Test
    void assignsAndListsStorefrontProducts() {
        commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce04", "b2c-main", "B2C Main", "USD", "en-US", true)
        );
        productCatalog.registerProduct(
            new RegisterProductCommand("arc-100", "Arc Product", "ARC", "Arc Category", new BigDecimal("10.00"), "USD")
        );
        productCatalog.registerProduct(
            new RegisterProductCommand("arc-200", "Arc Product 2", "ARC2", "Arc Category 2", new BigDecimal("12.00"), "USD")
        );

        StorefrontProductView assigned = commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand("commerce04", "b2c-main", "arc-100", "Featured Product", 1, true)
        );
        commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand("commerce04", "b2c-main", "arc-200", null, 2, false)
        );

        var listed = commerceCatalog.listStorefrontProducts("commerce04", "b2c-main", new PageQuery(0, 10), true);

        assertThat(assigned.storefrontCode()).isEqualTo("B2C-MAIN");
        assertThat(assigned.sku()).isEqualTo("ARC-100");
        assertThat(assigned.currentOrderability()).isEqualTo(ProductOrderability.ORDERABLE);
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(StorefrontProductView::sku).containsExactly("ARC-100");
    }

    @Test
    void rejectsDuplicateStorefrontSkuAssignments() {
        commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce05", "b2c-main", "B2C Main", "USD", "en-US", true)
        );
        productCatalog.registerProduct(
            new RegisterProductCommand("arc-500", "Arc Product 500", "ARC5", "Arc Category 5", new BigDecimal("10.00"), "USD")
        );
        commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand("commerce05", "b2c-main", "arc-500", null, 0, true)
        );

        assertThatThrownBy(() -> commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand("commerce05", "b2c-main", "ARC-500", null, 1, true)
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Storefront product already exists for tenant/storefront/sku: COMMERCE05/B2C-MAIN/ARC-500");
    }

    @Test
    void rejectsUnknownStorefrontProductSku() {
        commerceCatalog.createStorefront(
            new CreateStorefrontCommand("commerce06", "b2c-main", "B2C Main", "USD", "en-US", true)
        );

        assertThatThrownBy(() -> commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand("commerce06", "b2c-main", "missing-sku", null, 0, true)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("storefront product SKU not found: MISSING-SKU");
    }
}
