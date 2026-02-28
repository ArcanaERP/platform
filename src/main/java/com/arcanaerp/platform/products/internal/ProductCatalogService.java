package com.arcanaerp.platform.products.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.products.ChangeProductActivationCommand;
import com.arcanaerp.platform.products.ProductActivationChangeView;
import com.arcanaerp.platform.products.ProductCatalog;
import com.arcanaerp.platform.products.ProductLookup;
import com.arcanaerp.platform.products.ProductOrderability;
import com.arcanaerp.platform.products.ProductView;
import com.arcanaerp.platform.products.RegisterProductCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class ProductCatalogService implements ProductCatalog, ProductLookup {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final ProductActivationAuditRepository productActivationAuditRepository;
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

        return toView(product, category, price, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductView> listProducts(PageQuery pageQuery, Boolean active) {
        Page<Product> products = active == null
            ? productRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt")))
            : productRepository.findByActive(active, pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt")));

        Set<UUID> productIds = products.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> categoryIds = products.stream().map(Product::getCategoryId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Category> categoryById = new HashMap<>();
        categoryRepository.findAllById(categoryIds).forEach(category -> categoryById.put(category.getId(), category));
        Map<UUID, ProductActivationAudit> latestAuditByProductId = new HashMap<>();
        if (!productIds.isEmpty()) {
            productActivationAuditRepository.findByProductIdInOrderByChangedAtDesc(productIds)
                .forEach(audit -> latestAuditByProductId.putIfAbsent(audit.getProductId(), audit));
        }

        return PageResult.from(products).map(product -> {
                Category category = categoryById.get(product.getCategoryId());
                Price price = priceRepository.findTopByProductIdOrderByEffectiveFromDesc(product.getId()).orElse(null);
                return toView(product, category, price, latestAuditByProductId.get(product.getId()));
            });
    }

    @Override
    public ProductView changeProductActivation(ChangeProductActivationCommand command) {
        String normalizedSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeRequired(command.changedBy(), "changedBy");
        Product product = productRepository.findBySku(normalizedSku)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + normalizedSku));

        boolean targetActive = command.active();
        if (product.isActive() == targetActive) {
            throw new IllegalArgumentException("Product active flag is already " + targetActive);
        }

        Instant changedAt = Instant.now(clock);
        boolean previousActive = product.isActive();
        product.changeActivation(targetActive, changedAt);
        Product saved = productRepository.save(product);
        ProductActivationAudit audit = productActivationAuditRepository.save(
            ProductActivationAudit.create(saved.getId(), previousActive, targetActive, reason, changedBy, changedAt)
        );
        Category category = categoryRepository.findById(saved.getCategoryId()).orElse(null);
        Price price = priceRepository.findTopByProductIdOrderByEffectiveFromDesc(saved.getId()).orElse(null);
        return toView(saved, category, price, audit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductOrderability orderabilityOf(String sku) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        return productRepository.findBySku(normalizedSku)
            .map(product -> product.isActive() ? ProductOrderability.ORDERABLE : ProductOrderability.INACTIVE)
            .orElse(ProductOrderability.UNKNOWN);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductActivationChangeView> listActivationHistory(String sku, PageQuery pageQuery) {
        Product product = findProductBySku(sku);
        Page<ProductActivationAudit> audits = productActivationAuditRepository.findByProductId(
            product.getId(),
            PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        return PageResult.from(audits).map(audit -> new ProductActivationChangeView(
                audit.getId(),
                product.getSku(),
                audit.isPreviousActive(),
                audit.isCurrentActive(),
                audit.getReason(),
                audit.getChangedBy(),
                audit.getChangedAt()
            ));
    }

    private Product findProductBySku(String sku) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        return productRepository.findBySku(normalizedSku)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + normalizedSku));
    }

    private ProductView toView(Product product, Category category, Price price, ProductActivationAudit latestAudit) {
        return new ProductView(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.isActive(),
            product.getActivatedAt(),
            product.getDeactivatedAt(),
            latestAudit == null ? null : latestAudit.getReason(),
            latestAudit == null ? null : latestAudit.getChangedAt(),
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
