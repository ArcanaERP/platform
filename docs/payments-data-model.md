# Payments Data Model

Updated: 2026-03-12

## Scope

Current payments slice covers recording payments against issued invoices and reading current invoice balance.

## Aggregate

### Payment

Purpose:
- record a posted payment against an invoice
- provide the source rows used to calculate invoice balance

Core fields:
- `id` (`UUID`)
- `tenantCode`
- `paymentReference`
- `invoiceNumber`
- `amount`
- `currencyCode`
- `paidAt`
- `createdAt`

Rules:
- `tenantCode`, `paymentReference`, and `invoiceNumber` are normalized to uppercase
- source invoice must already be in `ISSUED` state
- payment currency must match invoice currency
- payment amount must be positive
- payment amount may not exceed current outstanding invoice balance

## Read Model

### InvoiceBalance

Purpose:
- expose a small receivables-style balance view without introducing a broader accounting module yet

Fields:
- `invoiceNumber`
- `currencyCode`
- `totalAmount`
- `paidAmount`
- `outstandingAmount`
- `paidInFull`

Computation:
- `paidAmount` is the sum of posted `payments.amount` for the invoice
- `outstandingAmount = totalAmount - paidAmount`

## Cross-Module Dependency

- `payments` reads invoices through public `InvoiceManagement`
- no dependency on `invoicing.internal`

## Minimal HTTP Surface

- `POST /api/payments`
- `GET /api/payments/invoices/{invoiceNumber}/balance`
- `GET /api/payments?page=&size=&invoiceNumber=&tenantCode=&paidAtFrom=&paidAtTo=`
- `GET /api/payments/tenants/{tenantCode}/summary?currencyCode=&paidAtFrom=&paidAtTo=`
- `GET /api/payments/tenants/{tenantCode}/invoices?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/daily-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/monthly-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/weekly-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`

## Query Notes

- payment listing is newest-first by `paidAt DESC`, then `createdAt DESC`
- optional filters currently support `invoiceNumber`, `tenantCode`, `paidAtFrom`, and `paidAtTo`
- blank query values and invalid/range-reversed timestamps are rejected at the HTTP boundary

## Tenant Summary

Purpose:
- expose a small reconciliation aggregate for a tenant in a single currency

Fields:
- `tenantCode`
- `currencyCode`
- `paymentCount`
- `invoiceCount`
- `totalCollected`

Notes:
- summary is intentionally scoped by `currencyCode` to avoid mixing monetary totals across currencies

## Tenant Invoice Breakdown

Purpose:
- show per-invoice collected totals for a tenant in a single currency

Fields:
- `tenantCode`
- `currencyCode`
- `invoiceNumber`
- `paymentCount`
- `totalCollected`

Notes:
- rows are grouped by `invoiceNumber`
- endpoint is paged for operational reconciliation screens

## Daily Tenant Summary

Purpose:
- provide a simple trend view of payment activity by UTC business date

Fields:
- `tenantCode`
- `currencyCode`
- `businessDate`
- `paymentCount`
- `invoiceCount`
- `totalCollected`

Notes:
- buckets are derived in the service layer from `paidAt` using UTC date semantics
- pagination applies to the date buckets after grouping

## Weekly Tenant Summary

Purpose:
- provide a mid-granularity trend view of payment activity by UTC business week

Fields:
- `tenantCode`
- `currencyCode`
- `businessWeekStart`
- `paymentCount`
- `invoiceCount`
- `totalCollected`

Notes:
- buckets are derived in the service layer from `paidAt` using UTC week semantics with Monday as the business-week start
- pagination applies to the week buckets after grouping

## Monthly Tenant Summary

Purpose:
- provide a coarse-grained trend view of payment activity by UTC business month

Fields:
- `tenantCode`
- `currencyCode`
- `businessMonth`
- `paymentCount`
- `invoiceCount`
- `totalCollected`

Notes:
- buckets are derived in the service layer from `paidAt` using UTC `YearMonth` semantics
- pagination applies to the month buckets after grouping
