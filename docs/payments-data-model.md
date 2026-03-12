# Payments Data Model

Updated: 2026-03-11

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

## Query Notes

- payment listing is newest-first by `paidAt DESC`, then `createdAt DESC`
- optional filters currently support `invoiceNumber`, `tenantCode`, `paidAtFrom`, and `paidAtTo`
- blank query values and invalid/range-reversed timestamps are rejected at the HTTP boundary
