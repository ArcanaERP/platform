package com.arcanaerp.platform.products.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.products.ProductCatalog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductCatalogServiceIntegrationTest {

    @Autowired
    private ProductCatalog productCatalog;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Test
    void listsPricesNewestFirstWithExpectedPaginationMetadata() {
        Instant base = Instant.parse("2026-03-04T00:00:00Z");

        Category category = categoryRepository.save(Category.create("KITS", "Kits", base));
        Product product = productRepository.save(Product.create("ARC-SVC-001", "Service Catalog Kit", category.getId(), base));
        priceRepository.save(Price.create(product.getId(), new BigDecimal("11.00"), "USD", base.plusSeconds(60)));
        priceRepository.save(Price.create(product.getId(), new BigDecimal("22.00"), "USD", base.plusSeconds(120)));
        priceRepository.save(Price.create(product.getId(), new BigDecimal("33.00"), "USD", base.plusSeconds(180)));

        var firstPage = productCatalog.listPrices("arc-svc-001", new PageQuery(0, 2));

        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalItems()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.items()).extracting(price -> price.amount().toPlainString()).containsExactly("33.0000", "22.0000");

        var secondPage = productCatalog.listPrices("arc-svc-001", new PageQuery(1, 2));

        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(secondPage.totalItems()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();
        assertThat(secondPage.items()).extracting(price -> price.amount().toPlainString()).containsExactly("11.0000");
    }

    @Test
    void throwsWhenListingPricesForUnknownSku() {
        assertThatThrownBy(() -> productCatalog.listPrices("missing-sku", new PageQuery(0, 10)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Product not found: MISSING-SKU");
    }
}
