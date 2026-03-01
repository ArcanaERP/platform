# Arcana ERP Modulith Module Map

Updated: 2026-03-01

## Scope

This map covers the currently implemented Spring Modulith modules under `com.arcanaerp.platform`:

- `core`
- `identity`
- `products`
- `orders`
- `inventory`

## Dependency Graph

Consumer -> allowed dependency

- `identity` -> `core::pagination`
- `products` -> `core::pagination`, `identity`
- `orders` -> `core::pagination`, `products`
- `inventory` -> *(none)*

Notes:

- `core::pagination` is a named interface exported from `com.arcanaerp.platform.core.pagination`.
- No module is allowed to depend on another module's `.internal` package.

## Module Boundaries

| Module | Package | Responsibilities | Public API (examples) | HTTP Surface |
| --- | --- | --- | --- | --- |
| Core | `com.arcanaerp.platform.core` | Shared primitives and cross-cutting infrastructure (API errors, UTC clock, pagination) | `PageQuery`, `PageResult` (via `core::pagination`) | Shared support only (no module-owned controller) |
| Identity | `com.arcanaerp.platform.identity` | Tenant/role/user primitives, org-unit directory, cross-module actor lookup | `UserDirectory`, `OrgUnitDirectory`, `IdentityActorLookup` | `POST /api/identity/users`, `GET /api/identity/users` |
| Products | `com.arcanaerp.platform.products` | Product catalog, activation lifecycle, activation audit history, orderability lookup | `ProductCatalog`, `ProductLookup` | `POST /api/products`, `GET /api/products`, `PATCH /api/products/{sku}/active`, `GET /api/products/{sku}/activation-history` |
| Orders | `com.arcanaerp.platform.orders` | Sales order creation, listing, and status transitions | `OrderManagement` | `POST /api/orders`, `GET /api/orders`, `PATCH /api/orders/{orderNumber}/status` |
| Inventory | `com.arcanaerp.platform.inventory` | On-hand inventory lookup by SKU | `InventoryAvailability` | `GET /api/inventory/{sku}` |

## Boundary Rules In Use

- Root module packages expose contracts (`*Command`, `*View`, service interfaces).
- Persistence/domain implementations stay in `.internal`.
- Cross-module calls are made only through exported module APIs:
  - `products` -> `identity` via `IdentityActorLookup`
  - `orders` -> `products` via `ProductLookup`
- Shared paging contract is centralized in `core::pagination`.
