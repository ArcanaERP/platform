# Products Module Data Model (High-Level)

Updated: 2026-03-01

## Entity Diagram

```mermaid
erDiagram
    PRODUCT_CATEGORIES ||--o{ PRODUCTS : classifies
    PRODUCTS ||--o{ PRODUCT_PRICES : has_prices
    PRODUCTS ||--o{ PRODUCT_ACTIVATION_AUDITS : has_activation_events

    PRODUCT_CATEGORIES {
      UUID id PK
      STRING code UK
      STRING name
      INSTANT createdAt
    }

    PRODUCTS {
      UUID id PK
      STRING sku UK
      STRING name
      UUID categoryId
      BOOLEAN active
      INSTANT activatedAt
      INSTANT deactivatedAt
      INSTANT createdAt
    }

    PRODUCT_PRICES {
      UUID id PK
      UUID productId
      DECIMAL amount
      STRING currencyCode
      INSTANT effectiveFrom
    }

    PRODUCT_ACTIVATION_AUDITS {
      UUID id PK
      UUID productId
      BOOLEAN previousActive
      BOOLEAN currentActive
      STRING reason
      STRING tenantCode
      STRING changedBy
      INSTANT changedAt
    }
```

## Relationship Notes

- `products.categoryId` is a logical reference to `product_categories.id` (explicit ID link, no bidirectional JPA mapping).
- `product_prices.productId` is a logical reference to `products.id`.
- `product_activation_audits.productId` is a logical reference to `products.id`.

## Query and Index Notes

- `product_prices`: index on `(productId, effectiveFrom)` for latest price lookups.
- `product_activation_audits`: indexes on:
  - `(productId, changedAt)`
  - `(productId, currentActive, changedAt)`
  - `(productId, tenantCode, changedAt)`
  - `(productId, changedBy, changedAt)`
  - `(productId, tenantCode, changedBy, changedAt)`
