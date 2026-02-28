package com.arcanaerp.platform.products.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.products.ProductCatalog;
import com.arcanaerp.platform.products.ProductView;
import com.arcanaerp.platform.products.RegisterProductCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class ProductCatalogService implements ProductCatalog {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final Clock clock;

    @Override
    public ProductView registerProduct(RegisterProductCommand command) {
        String sku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String productName = normalizeRequired(command.name(), "name");
        String categoryCode = normalizeRequired(command.categoryCode(), "categoryCode").toUpperCase();
        String categoryName = normalizeRequired(command.categoryName(), "categoryName");
        Instant now = Instant.now(clock);

        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("Product SKU already exists: " + sku);
        }

        Category category = categoryRepository.findByCode(categoryCode)
            .orElseGet(() -> categoryRepository.save(Category.create(categoryCode, categoryName, now)));

        Product product = productRepository.save(Product.create(sku, productName, category.getId(), now));
        Price price = priceRepository.save(
            Price.create(product.getId(), command.amount(), command.currencyCode(), now)
        );

        return toView(product, category, price);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductView> listProducts(PageQuery pageQuery) {
        Page<Product> products = productRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Set<UUID> categoryIds = products.stream().map(Product::getCategoryId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Category> categoryById = new HashMap<>();
        categoryRepository.findAllById(categoryIds).forEach(category -> categoryById.put(category.getId(), category));

        return PageResult.from(products).map(product -> {
                Category category = categoryById.get(product.getCategoryId());
                Price price = priceRepository.findTopByProductIdOrderByEffectiveFromDesc(product.getId()).orElse(null);
                return toView(product, category, price);
            });
    }

    private ProductView toView(Product product, Category category, Price price) {
        return new ProductView(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategoryId(),
            category == null ? null : category.getCode(),
            category == null ? null : category.getName(),
            price == null ? null : price.getAmount(),
            price == null ? null : price.getCurrencyCode(),
            price == null ? null : price.getEffectiveFrom()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
