# Arcana ERP Modulith Module Map

Updated: 2026-03-14

## Scope

This map covers the currently implemented Spring Modulith modules under `com.arcanaerp.platform`:

- `core`
- `identity`
- `products`
- `orders`
- `invoicing`
- `payments`
- `agreements`
- `inventory`

## Dependency Graph

Consumer -> allowed dependency

- `identity` -> `core::pagination`
- `products` -> `core::pagination`, `identity`
- `orders` -> `core::pagination`, `products`
- `invoicing` -> `core::pagination`, `orders`
- `payments` -> `identity`, `invoicing`
- `agreements` -> `core::pagination`, `identity`
- `inventory` -> `core::pagination`, `core::errors`

Notes:

- `core::pagination` is a named interface exported from `com.arcanaerp.platform.core.pagination`.
- `core::errors` is a named interface exported from `com.arcanaerp.platform.core.api`.
- No module is allowed to depend on another module's `.internal` package.

## Module Boundaries

| Module | Package | Responsibilities | Public API (examples) | HTTP Surface |
| --- | --- | --- | --- | --- |
| Core | `com.arcanaerp.platform.core` | Shared primitives and cross-cutting infrastructure (API errors, UTC clock, pagination) | `PageQuery`, `PageResult` (via `core::pagination`) | Shared support only (no module-owned controller) |
| Identity | `com.arcanaerp.platform.identity` | Tenant/role/user primitives, org-unit directory, cross-module actor lookup | `UserDirectory`, `OrgUnitDirectory`, `IdentityActorLookup` | `POST /api/identity/users`, `GET /api/identity/users` |
| Products | `com.arcanaerp.platform.products` | Product catalog, activation lifecycle, activation audit history, orderability lookup | `ProductCatalog`, `ProductLookup` | `POST /api/products`, `GET /api/products`, `PATCH /api/products/{sku}/active`, `GET /api/products/{sku}/activation-history` |
| Orders | `com.arcanaerp.platform.orders` | Sales order creation, listing, and status transitions | `OrderManagement` | `POST /api/orders`, `GET /api/orders`, `PATCH /api/orders/{orderNumber}/status` |
| Invoicing | `com.arcanaerp.platform.invoicing` | Order-backed invoice creation, direct retrieval, filtered paged listing, minimal lifecycle transitions (`DRAFT -> ISSUED`, `DRAFT/ISSUED -> VOID`; `VOID` final), and immutable status-change history | `InvoiceManagement` | `POST /api/invoices`, `GET /api/invoices/{invoiceNumber}`, `GET /api/invoices`, `PATCH /api/invoices/{invoiceNumber}/status`, `GET /api/invoices/{invoiceNumber}/status-history` |
| Payments | `com.arcanaerp.platform.payments` | Payment capture against issued invoices plus invoice balance, tenant receivables read models, tenant receivables summary totals, tenant receivables aging buckets, per-bucket drill-down, over-90 collections queue reads with due-date cutoff, assignee triage, follow-up-date filtering, and optional follow-up-driven prioritization, assignment workflow for queue ownership, follow-up scheduling and completion on assigned collections work with per-invoice follow-up history, append-only invoice notes/worklog with tenant-wide feed reads and assignee-scoped filtering, structured category/outcome metadata, outcome-based and category-based note summaries with assignee-scoped filtering, daily/weekly/monthly note activity summaries, daily/weekly/monthly category note trend summaries, daily/weekly/monthly collections note category-outcome cross-tab summaries, daily/weekly/monthly note outcome trend summaries, per-invoice and tenant-level assignment history with assignee/date filters, current workload summary by assignee, daily/weekly/monthly assignment activity trends, paged reconciliation reads, tenant-scoped collection summary, invoice-level tenant breakdown, and UTC day/week/month-bucketed tenant trends | `PaymentManagement` | `POST /api/payments`, `GET /api/payments/invoices/{invoiceNumber}/balance`, `GET /api/payments/tenants/{tenantCode}/receivables`, `GET /api/payments/tenants/{tenantCode}/receivables/summary`, `GET /api/payments/tenants/{tenantCode}/receivables/aging`, `GET /api/payments/tenants/{tenantCode}/receivables/aging/{agingBucket}`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90`, `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment`, `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up`, `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up/complete`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up-history`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment-history`, `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/daily-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/monthly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/daily-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/monthly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/daily-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/monthly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/daily-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/monthly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignment-history`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/daily-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/receivables/collections/monthly-summary`, `GET /api/payments`, `GET /api/payments/tenants/{tenantCode}/summary`, `GET /api/payments/tenants/{tenantCode}/invoices`, `GET /api/payments/tenants/{tenantCode}/daily-summary`, `GET /api/payments/tenants/{tenantCode}/weekly-summary`, `GET /api/payments/tenants/{tenantCode}/monthly-summary` |
| Agreements | `com.arcanaerp.platform.agreements` | Agreement master-record creation, direct retrieval by agreement number, paged listing (optional status filtering), lifecycle transitions (`DRAFT -> ACTIVE`, `DRAFT/ACTIVE -> TERMINATED`; `TERMINATED` final), and immutable status-change history with tenant-scoped actor/reason attribution and optional audit filters | `AgreementManagement` | `POST /api/agreements`, `GET /api/agreements/{agreementNumber}`, `GET /api/agreements`, `PATCH /api/agreements/{agreementNumber}/status`, `GET /api/agreements/{agreementNumber}/status-history` |
| Inventory | `com.arcanaerp.platform.inventory` | On-hand inventory by `sku + location`, location-scoped adjustment transactions, and location-to-location transfers with optional source-document references and idempotent reversal retries with payload-consistency, concurrent first-write conflict enforcement, and stale-claim recovery | `InventoryAvailability` | `GET /api/inventory/{sku}`, `POST /api/inventory/{sku}/adjustments`, `GET /api/inventory/{sku}/adjustments`, `POST /api/inventory/{sku}/transfers`, `GET /api/inventory/transfers/{transferId}`, `POST /api/inventory/transfers/{transferId}/reversals`, `GET /api/inventory/transfers/{transferId}/reversals`, `GET /api/inventory/{sku}/transfers` |

## Boundary Rules In Use

- Root module packages expose contracts (`*Command`, `*View`, service interfaces).
- Persistence/domain implementations stay in `.internal`.
- Cross-module calls are made only through exported module APIs:
  - `products` -> `identity` via `IdentityActorLookup`
  - `agreements` -> `identity` via `IdentityActorLookup`
  - `orders` -> `products` via `ProductLookup`
  - `invoicing` -> `orders` via `OrderManagement`
  - `payments` -> `invoicing` via `InvoiceManagement`
  - `payments` -> `identity` via `IdentityActorLookup`
- Shared paging contract is centralized in `core::pagination`.
