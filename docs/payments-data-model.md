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
- `assignedTo` (nullable)
- `assignedBy` (nullable)
- `assignedAt` (nullable)

Computation:
- source invoices come from `InvoiceManagement.listInvoices(...)` filtered to `status=ISSUED`
- only invoices with positive `outstandingAmount` are eligible
- `agingBucket` is derived from UTC `daysPastDue`

Notes:
- bucket drill-down is paged after filtering and ordered by `dueAt ASC`, then `invoiceNumber ASC`
- `daysPastDue <= 0` maps to `CURRENT`
- assignment metadata is joined in from the current collections assignment state when present

### Over90CollectionsQueue

Purpose:
- expose a dedicated collections work queue for invoices more than 90 days past due

Fields:
- same row shape as `AgedTenantReceivable`

Filters:
- `invoiceNumber` exact match, optional
- `assignedTo` exact assignee email, optional
- `dueAtOnOrBefore` UTC instant cutoff, optional

Notes:
- queue is a shortcut over the `OVERDUE_OVER_90` aging bucket
- rows remain ordered by `dueAt ASC`, then `invoiceNumber ASC`
- blank `invoiceNumber` query values are rejected at the HTTP boundary
- blank `assignedTo` query values are rejected at the HTTP boundary
- invalid `dueAtOnOrBefore` values are rejected at the HTTP boundary
- queue rows now include current assignment metadata when present

### CollectionsAssignment

Purpose:
- persist the current owner of a collections invoice inside `payments`

Fields:
- `tenantCode`
- `invoiceNumber`
- `assignedTo`
- `assignedBy`
- `assignedAt`

Rules:
- assignment is only allowed for invoices currently in the over-90 queue
- `assignedTo` and `assignedBy` must both resolve through `IdentityActorLookup`
- one current assignment row exists per `invoiceNumber`; reassignment updates that row

### CollectionsAssignmentAudit

Purpose:
- preserve an append-only ownership trail for collections assignments

Fields:
- `id` (`UUID`)
- `tenantCode`
- `invoiceNumber`
- `assignedTo`
- `assignedBy`
- `assignedAt`

Rules:
- every successful assignment write appends one audit row
- history is read newest-first by `assignedAt`
- history remains tenant-scoped and invoice-scoped
- history filters currently support optional exact `assignedTo` plus `assignedAtFrom` / `assignedAtTo`
- tenant history also supports optional exact `invoiceNumber`

### CollectionsFollowUpAudit

Purpose:
- preserve an append-only history of follow-up scheduling and rescheduling

Fields:
- `id` (`UUID`)
- `tenantCode`
- `invoiceNumber`
- `previousFollowUpAt`
- `followUpAt`
- `outcome`
- `changedBy`
- `changedAt`

Rules:
- every successful follow-up schedule, reschedule, or completion appends one audit row
- history is read newest-first by `changedAt`
- history remains tenant-scoped and invoice-scoped
- completion rows clear current assignment follow-up state and use `followUpAt = null`
- completion rows require an outcome such as `CONTACTED`, `LEFT_VOICEMAIL`, `PROMISE_TO_PAY`, or `NO_RESPONSE`

### TenantCollectionsAssignmentSummary

Purpose:
- expose the current over-90 collections workload grouped by assignee

Fields:
- `tenantCode`
- `currencyCode`
- `assignedTo` (nullable for unassigned bucket)
- `assignedInvoiceCount`
- `totalOutstandingAmount`
- `oldestDueAt`

Rules:
- summary is built from current over-90 queue rows, not from historical audit rows
- unassigned invoices are grouped into a nullable `assignedTo` bucket
- rows are ordered by `assignedTo` ascending with the unassigned bucket last

### DailyTenantCollectionsAssignmentSummary

Purpose:
- expose day-bucketed assignment activity for a tenant from the append-only audit trail

Fields:
- `tenantCode`
- `businessDate`
- `assignmentCount`
- `invoiceCount`

Rules:
- summary is built from `CollectionsAssignmentAudit` rows using UTC `LocalDate` buckets
- optional filters currently support exact `assignedTo` plus `assignedAtFrom` / `assignedAtTo`
- `invoiceCount` counts distinct invoices touched on that day

### WeeklyTenantCollectionsAssignmentSummary

Purpose:
- expose week-bucketed assignment activity for a tenant from the append-only audit trail

Fields:
- `tenantCode`
- `businessWeekStart`
- `assignmentCount`
- `invoiceCount`

Rules:
- summary is built from `CollectionsAssignmentAudit` rows using UTC business-week buckets
- business weeks start on Monday
- optional filters currently support exact `assignedTo` plus `assignedAtFrom` / `assignedAtTo`
- `invoiceCount` counts distinct invoices touched in that week

### MonthlyTenantCollectionsAssignmentSummary

Purpose:
- expose month-bucketed assignment activity for a tenant from the append-only audit trail

Fields:
- `tenantCode`
- `businessMonth`
- `assignmentCount`
- `invoiceCount`

Rules:
- summary is built from `CollectionsAssignmentAudit` rows using UTC `YearMonth` buckets
- optional filters currently support exact `assignedTo` plus `assignedAtFrom` / `assignedAtTo`
- `invoiceCount` counts distinct invoices touched in that month

### CollectionsNote

Purpose:
- capture append-only collections worklog entries against an assigned over-90 invoice

Fields:
- `id`
- `tenantCode`
- `invoiceNumber`
- `note`
- `notedBy`
- `category`
- `outcome`
- `notedAt`

Rules:
- note creation requires the invoice to still be in the over-90 queue and to already have a current assignment
- `notedBy` must resolve through the public `IdentityActorLookup`
- notes are immutable and exposed newest-first with optional `notedBy`, `category`, `outcome`, and noted-at range filters
- tenant-wide notes feed also supports optional exact `invoiceNumber` filtering and exact current `assignedTo` filtering
- `assignedTo` is resolved against the current `CollectionsAssignment` row for each invoice, not against note authorship

### TenantCollectionsNoteOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by `outcome`

Fields:
- `tenantCode`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `category`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one outcome bucket

### TenantCollectionsNoteCategorySummary

Purpose:
- expose tenant-scoped collections note activity grouped by `category`

Fields:
- `tenantCode`
- `category`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `outcome`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one category bucket

### DailyTenantCollectionsNoteCategorySummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business date and note `category`

Fields:
- `tenantCode`
- `businessDate`
- `category`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `outcome`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessDate + category` bucket

### WeeklyTenantCollectionsNoteCategorySummary

Purpose:
- expose tenant-scoped collections note activity grouped by Monday-based UTC business week and note `category`

Fields:
- `tenantCode`
- `businessWeekStart`
- `category`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `outcome`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessWeekStart + category` bucket

### MonthlyTenantCollectionsNoteCategorySummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC `YearMonth` and note `category`

Fields:
- `tenantCode`
- `businessMonth`
- `category`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `outcome`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessMonth + category` bucket

### DailyTenantCollectionsNoteCategoryOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business date plus note `category` and `outcome`

Fields:
- `tenantCode`
- `businessDate`
- `category`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessDate + category + outcome` bucket

### WeeklyTenantCollectionsNoteCategoryOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by Monday-based UTC business week plus note `category` and `outcome`

Fields:
- `tenantCode`
- `businessWeekStart`
- `category`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessWeekStart + category + outcome` bucket

### MonthlyTenantCollectionsNoteCategoryOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC `YearMonth` plus note `category` and `outcome`

Fields:
- `tenantCode`
- `businessMonth`
- `category`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessMonth + category + outcome` bucket

### DailyTenantCollectionsNoteSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business date

Fields:
- `tenantCode`
- `businessDate`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `category`, exact `outcome`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one UTC business-date bucket

### WeeklyTenantCollectionsNoteSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business week

Fields:
- `tenantCode`
- `businessWeekStart`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `category`, exact `outcome`, and noted-at range bounds
- week buckets use Monday as `businessWeekStart`
- `invoiceCount` counts distinct invoices touched within one UTC business-week bucket

### MonthlyTenantCollectionsNoteSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business month

Fields:
- `tenantCode`
- `businessMonth`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters currently support exact current `assignedTo`, exact `notedBy`, exact `category`, exact `outcome`, and noted-at range bounds
- month buckets use UTC `YearMonth`
- `invoiceCount` counts distinct invoices touched within one UTC business-month bucket

## Cross-Module Dependency

- `payments` reads invoices through public `InvoiceManagement`
- `payments` validates collections actors through public `IdentityActorLookup`
- no dependency on `invoicing.internal`

## Minimal HTTP Surface

- `POST /api/payments`
- `GET /api/payments/invoices/{invoiceNumber}/balance`
- `GET /api/payments/tenants/{tenantCode}/receivables?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/summary?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging/{agingBucket}?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90?currencyCode=&invoiceNumber=&assignedTo=&dueAtOnOrBefore=&followUpAtFrom=&followUpAtTo=&followUpScheduled=&latestFollowUpOutcome=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome-summary?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/daily-summary?page=&size=&assignedTo=&outcome=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/weekly-summary?page=&size=&assignedTo=&outcome=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/monthly-summary?page=&size=&assignedTo=&outcome=&changedBy=&changedAtFrom=&changedAtTo=`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up/complete`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/follow-up-history?page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment-history?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes?page=&size=&notedBy=&category=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes?page=&size=&invoiceNumber=&assignedTo=&notedBy=&category=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome-summary?page=&size=&assignedTo=&notedBy=&category=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-summary?page=&size=&assignedTo=&notedBy=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/daily-summary?page=&size=&assignedTo=&notedBy=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/weekly-summary?page=&size=&assignedTo=&notedBy=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category/monthly-summary?page=&size=&assignedTo=&notedBy=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/daily-summary?page=&size=&assignedTo=&notedBy=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/weekly-summary?page=&size=&assignedTo=&notedBy=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/category-outcome/monthly-summary?page=&size=&assignedTo=&notedBy=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/daily-summary?page=&size=&assignedTo=&notedBy=&category=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/weekly-summary?page=&size=&assignedTo=&notedBy=&category=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/monthly-summary?page=&size=&assignedTo=&notedBy=&category=&outcome=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/daily-summary?page=&size=&assignedTo=&notedBy=&category=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/weekly-summary?page=&size=&assignedTo=&notedBy=&category=&notedAtFrom=&notedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/notes/outcome/monthly-summary?page=&size=&assignedTo=&notedBy=&category=&notedAtFrom=&notedAtTo=`

### DailyTenantCollectionsNoteOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC business date and note `outcome`

Fields:
- `tenantCode`
- `businessDate`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters support exact current `assignedTo`, exact `notedBy`, exact `category`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessDate + outcome` bucket

### WeeklyTenantCollectionsNoteOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by Monday-based UTC business week and note `outcome`

Fields:
- `tenantCode`
- `businessWeekStart`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters support exact current `assignedTo`, exact `notedBy`, exact `category`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessWeekStart + outcome` bucket

### MonthlyTenantCollectionsNoteOutcomeSummary

Purpose:
- expose tenant-scoped collections note activity grouped by UTC `YearMonth` and note `outcome`

Fields:
- `tenantCode`
- `businessMonth`
- `outcome`
- `noteCount`
- `invoiceCount`

Rules:
- summary is built from immutable `CollectionsNote` rows
- optional filters support exact current `assignedTo`, exact `notedBy`, exact `category`, and noted-at range bounds
- `invoiceCount` counts distinct invoices touched within one `businessMonth + outcome` bucket
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignment-history?page=&size=&invoiceNumber=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/summary?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/daily-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/weekly-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/monthly-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
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
