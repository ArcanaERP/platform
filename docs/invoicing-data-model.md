# Invoicing Data Model

Updated: 2026-03-13

## Scope

Current invoicing slice covers a minimal invoice header derived from a confirmed sales order.

## Aggregate

### Invoice

Purpose:
- represent a billable document tied to an order
- hold due-date and lifecycle state needed for a first invoice workflow

Core fields:
- `id` (`UUID`)
- `tenantCode`
- `invoiceNumber`
- `orderNumber`
- `status` (`DRAFT`, `ISSUED`, `VOID`)
- `currencyCode`
- `totalAmount`
- `createdAt`
- `dueAt`
- `issuedAt`
- `voidedAt`

Rules:
- `tenantCode`, `invoiceNumber`, and `orderNumber` are normalized to uppercase
- source order must already be in `CONFIRMED` state
- `currencyCode` and `totalAmount` are copied from the source order at invoice creation time
- invoice responses include immutable line snapshots copied from the source order at invoice creation time
- lifecycle is intentionally minimal:
  - `DRAFT -> ISSUED`
  - `DRAFT -> VOID`
  - `ISSUED -> VOID`
  - `VOID` is terminal

### InvoiceStatusChangeAudit

Purpose:
- keep lifecycle transitions append-only and queryable for audit/history views

Core fields:
- `id` (`UUID`)
- `invoiceId`
- `previousStatus`
- `currentStatus`
- `reason`
- `changedBy`
- `changedAt`

Rules:
- status-change audits capture trimmed reason and normalized lowercase `changedBy` actor email
- status-change actors must exist in the invoice tenant through `identity::IdentityActorLookup`

### InvoiceLine

Purpose:
- preserve invoice line detail independently of later order or catalog changes

Core fields:
- `id` (`UUID`)
- `invoiceId`
- `lineNo`
- `productSku`
- `quantity`
- `unitPrice`
- `lineTotal`
- `createdAt`

## Cross-Module Dependency

- `invoicing` reads `orders` through public `OrderManagement`
- `invoicing` validates status-change actors through public `IdentityActorLookup`
- no dependency on `orders.internal`

## Minimal HTTP Surface

- `POST /api/invoices`
- `GET /api/invoices/{invoiceNumber}`
- `GET /api/invoices?page=&size=&tenantCode=&status=&currencyCode=`
- `GET /api/invoices/status-activity/daily-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/invoices/status-activity/weekly-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/invoices/status-activity/monthly-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `PATCH /api/invoices/{invoiceNumber}/status` (request includes `status`, `reason`, `changedBy`)
- `GET /api/invoices/{invoiceNumber}/status-history?page=&size=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`

## Query Notes

- invoice listing is newest-first by `createdAt DESC`
- optional invoice-list filters currently support `tenantCode`, `status`, and `currencyCode`
- blank query values are rejected at the HTTP boundary
- invoice status history supports optional `previousStatus`, `currentStatus`, `changedBy`, `changedAtFrom`, and `changedAtTo` filters
- status-history ranges require `changedAtFrom <= changedAtTo`
- status-activity summaries use UTC daily, Monday-start weekly, and calendar-month buckets
- status-activity summaries support optional `tenantCode`, `previousStatus`, `currentStatus`, `changedBy`, `changedAtFrom`, and `changedAtTo` filters
- status-activity summaries return transition counts plus distinct invoice counts
