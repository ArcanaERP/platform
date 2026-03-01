# Inventory Module Data Model (High-Level)

Updated: 2026-03-01

## Entity Diagram

```mermaid
erDiagram
    INVENTORY_ITEMS ||--o{ INVENTORY_ADJUSTMENTS : records_movements

    INVENTORY_ITEMS {
      UUID id PK
      STRING sku UK
      DECIMAL onHandQuantity
      INSTANT updatedAt
    }

    INVENTORY_ADJUSTMENTS {
      UUID id PK
      UUID inventoryItemId
      STRING sku
      DECIMAL previousOnHandQuantity
      DECIMAL quantityDelta
      DECIMAL currentOnHandQuantity
      STRING reason
      STRING adjustedBy
      INSTANT adjustedAt
    }
```

## Relationship Notes

- `inventory_items.sku` is a business-key link to product SKU semantics (no cross-module foreign key).
- `inventory_adjustments.inventoryItemId` is a logical reference to `inventory_items.id`.
- Inventory changes are append-only via `inventory_adjustments`; `inventory_items.onHandQuantity` stores latest state.

## Constraint Notes

- Unique constraints:
  - `inventory_items(sku)`
- Indexes:
  - `inventory_adjustments(inventoryItemId, adjustedAt)`
  - `inventory_adjustments(inventoryItemId, adjustedBy, adjustedAt)`
