# Payments Data Model

Updated: 2026-03-13

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

### TenantReceivable

Purpose:
- expose a tenant-scoped receivables worklist of issued invoices and their current outstanding balances

Fields:
- `tenantCode`
- `currencyCode`
- `invoiceNumber`
- `dueAt`
- `issuedAt`
- `totalAmount`
- `paidAmount`
- `outstandingAmount`
- `paidInFull`

Computation:
- source invoices come from `InvoiceManagement.listInvoices(...)` filtered to `status=ISSUED`
- `paidAmount` is the sum of posted `payments.amount` for the invoice
- `outstandingAmount = totalAmount - paidAmount`

### TenantReceivablesSummary

Purpose:
- expose tenant-scoped aggregate receivables totals for issued invoices in one currency

Fields:
- `tenantCode`
- `currencyCode`
- `invoiceCount`
- `totalAmount`
- `paidAmount`
- `outstandingAmount`
- `paidInFullCount`

Computation:
- source invoices come from `InvoiceManagement.listInvoices(...)` filtered to `status=ISSUED`
- `paidAmount` is the sum of posted `payments.amount` across those invoices
- `outstandingAmount = totalAmount - paidAmount`

Notes:
- summary is intentionally scoped by `currencyCode` to avoid mixing monetary totals across currencies
- the current implementation pages through issued invoices via the public invoicing API and aggregates results in the service layer

### TenantReceivablesAging

Purpose:
- expose tenant-scoped outstanding receivables in fixed aging bands for one currency

Fields:
- `tenantCode`
- `currencyCode`
- `asOfDate`
- `totalOutstandingInvoiceCount`
- `totalOutstandingAmount`
- `currentInvoiceCount`
- `currentAmount`
- `overdue1To30InvoiceCount`
- `overdue1To30Amount`
- `overdue31To60InvoiceCount`
- `overdue31To60Amount`
- `overdue61To90InvoiceCount`
- `overdue61To90Amount`
- `overdueOver90InvoiceCount`
- `overdueOver90Amount`

Computation:
- source invoices come from `InvoiceManagement.listInvoices(...)` filtered to `status=ISSUED`
- only invoices with positive `outstandingAmount` contribute to aging buckets
- aging compares invoice `dueAt` to the current UTC business date from the shared platform `Clock`

Notes:
- `current` means due today or in the future
- overdue bands are fixed at `1-30`, `31-60`, `61-90`, and `>90` days past due
- the current implementation pages through issued invoices via the public invoicing API and aggregates results in the service layer

### AgedTenantReceivable

Purpose:
- expose the invoice-level drill-down rows that back a single tenant aging bucket

Fields:
- `tenantCode`
- `currencyCode`
- `invoiceNumber`
- `dueAt`
- `issuedAt`
- `totalAmount`
- `paidAmount`
- `outstandingAmount`
- `asOfDate`
- `daysPastDue`
- `agingBucket`

Computation:
- source invoices come from `InvoiceManagement.listInvoices(...)` filtered to `status=ISSUED`
- only invoices with positive `outstandingAmount` are eligible
- `agingBucket` is derived from UTC `daysPastDue`

Notes:
- bucket drill-down is paged after filtering and ordered by `dueAt ASC`, then `invoiceNumber ASC`
- `daysPastDue <= 0` maps to `CURRENT`

### Over90CollectionsQueue

Purpose:
- expose a dedicated collections work queue for invoices more than 90 days past due

Fields:
- same row shape as `AgedTenantReceivable`

Filters:
- `invoiceNumber` exact match, optional

Notes:
- queue is a shortcut over the `OVERDUE_OVER_90` aging bucket
- rows remain ordered by `dueAt ASC`, then `invoiceNumber ASC`
- blank `invoiceNumber` query values are rejected at the HTTP boundary

## Cross-Module Dependency

- `payments` reads invoices through public `InvoiceManagement`
- no dependency on `invoicing.internal`

## Minimal HTTP Surface

- `POST /api/payments`
- `GET /api/payments/invoices/{invoiceNumber}/balance`
- `GET /api/payments/tenants/{tenantCode}/receivables?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/summary?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging/{agingBucket}?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90?currencyCode=&invoiceNumber=&page=&size=`
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
