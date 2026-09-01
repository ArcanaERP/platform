# Agreements Module Data Model (High-Level)

Updated: 2026-04-25

## Entity Diagram

```mermaid
erDiagram
    AGREEMENTS {
      UUID id PK
      STRING tenantCode
      STRING agreementNumber UK
      STRING name
      STRING agreementType
      STRING status
      INSTANT effectiveFrom
      INSTANT createdAt
      INSTANT activatedAt
      INSTANT terminatedAt
    }
    AGREEMENT_STATUS_CHANGE_AUDITS {
      UUID id PK
      UUID agreementId
      STRING previousStatus
      STRING currentStatus
      STRING tenantCode
      STRING reason
      STRING changedBy
      INSTANT changedAt
    }
    AGREEMENTS ||--o{ AGREEMENT_STATUS_CHANGE_AUDITS : "status changes"
```

## Relationship Notes

- This initial `agreements` slice models a single aggregate (`Agreement`) with no external entity links.
- `tenantCode` is normalized to uppercase and currently scopes list queries.
- `agreementNumber` is the external business identifier and is normalized to uppercase.
- `agreementNumber` is also the direct-read lookup key for `GET /api/agreements/{agreementNumber}`.
- `agreementType` is stored as an uppercase normalized string for consistent filtering/parity expansion.
- `status` starts as `DRAFT` and currently supports transitions:
  - `DRAFT -> ACTIVE`
  - `DRAFT -> TERMINATED`
  - `ACTIVE -> TERMINATED`
  - `TERMINATED` is final (except same-state no-op calls)
- Transition timestamps:
  - `activatedAt` set when agreement transitions to `ACTIVE`
  - `terminatedAt` set when agreement transitions to `TERMINATED`
  - when `ACTIVE -> TERMINATED`, `activatedAt` remains populated and `terminatedAt` is set
- Immutable status history:
  - each successful status transition appends one row to `agreement_status_change_audits`
  - transition requests require attribution metadata (`tenantCode`, `reason`, `changedBy`)
  - transitions validate `changedBy` against `identity` module actor lookup within `tenantCode`
  - history is exposed via `GET /api/agreements/{agreementNumber}/status-history?page=&size=&tenantCode=&changedBy=&changedAtFrom=&changedAtTo=`
  - history rows include `tenantCode`, `reason`, and normalized-lowercase `changedBy`
  - no-op transitions (`ACTIVE -> ACTIVE`, etc.) do not append history rows
- Listing/query behavior:
  - agreements list endpoint supports optional `tenantCode` and `status` filters (`DRAFT`, `ACTIVE`, `TERMINATED`)
  - list results are sorted by `createdAt DESC`
  - status-history results are sorted by `changedAt DESC`
  - status-history supports optional filters:
    - `tenantCode` (uppercase-normalized)
    - `changedBy` (lowercase-normalized)
    - `changedAtFrom` / `changedAtTo` (ISO-8601 instant range)
- Lifecycle activity summaries:
  - daily, weekly, and monthly summaries bucket status transitions by UTC `changedAt`
  - weekly buckets start on Monday
  - summary rows include `transitionCount` and distinct `agreementCount`
  - by-current-status summaries split each bucket by `currentStatus`
  - summary endpoints support optional `tenantCode`, `previousStatus`, `currentStatus`, `changedBy`, `changedAtFrom`, and `changedAtTo` filters

## Constraint Notes

- Unique constraints:
  - `agreements(agreementNumber)` remains globally unique during this early iteration
- Indexes:
  - `agreement_status_change_audits(agreementId, changedAt)`
  - `agreement_status_change_audits(agreementId, tenantCode, changedAt)`

## Minimal HTTP Surface

- `POST /api/agreements`
- `GET /api/agreements/{agreementNumber}`
- `GET /api/agreements?page=&size=&tenantCode=&status=`
- `GET /api/agreements/status-activity/daily-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/agreements/status-activity/daily-summary/by-current-status?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/agreements/status-activity/weekly-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/agreements/status-activity/weekly-summary/by-current-status?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/agreements/status-activity/monthly-summary?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/agreements/status-activity/monthly-summary/by-current-status?page=&size=&tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=`
- `PATCH /api/agreements/{agreementNumber}/status` (request includes `status`, `tenantCode`, `reason`, `changedBy`)
- `GET /api/agreements/{agreementNumber}/status-history?page=&size=&tenantCode=&changedBy=&changedAtFrom=&changedAtTo=`
