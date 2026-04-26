# Work Effort Data Model

Updated: 2026-04-25

## Scope

Current work-effort slice covers tenant-scoped work-effort registration, direct lookup, filtered listing, lightweight status transitions, and append-only status history.

## Aggregate

### WorkEffort

Purpose:
- represent a tenant-scoped unit of planned or active work
- hold assignment and due-date state needed for the current workflow

Core fields:
- `id` (`UUID`)
- `tenantCode`
- `effortNumber`
- `name`
- `description`
- `status`
- `assignedTo`
- `dueAt`
- `createdAt`

Rules:
- `tenantCode` and `effortNumber` are normalized to uppercase
- `assignedTo` is normalized to lowercase when present
- create and status-transition commands validate actor emails through the `identity` module
- list filters currently support exact `tenantCode`, optional exact `status`, and optional exact `assignedTo`

### WorkEffortStatusChangeAudit

Purpose:
- keep status transitions append-only and queryable for audit/history views

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `effortNumber`
- `previousStatus`
- `currentStatus`
- `tenantCode`
- `reason`
- `changedBy`
- `changedAt`

Rules:
- each successful status transition appends one audit row
- history reads are newest-first by `changedAt`
- optional history filters support exact `tenantCode`, exact `changedBy`, and `changedAtFrom` / `changedAtTo`

## Cross-Module Dependency

- `workeffort` validates assignees and status actors through public `IdentityActorLookup`
- no dependency on `identity.internal`

## Minimal HTTP Surface

- `POST /api/work-efforts`
- `GET /api/work-efforts/{effortNumber}?tenantCode=`
- `GET /api/work-efforts?tenantCode=&status=&assignedTo=&page=&size=`
- `PATCH /api/work-efforts/{effortNumber}/status` (request includes `tenantCode`, `status`, `reason`, `changedBy`)
- `GET /api/work-efforts/{effortNumber}/status-history?tenantCode=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`

## Query Notes

- work-effort listing is paged through the shared `PageQuery` contract
- blank query values are rejected at the HTTP boundary
- status-history ranges require `changedAtFrom <= changedAtTo`
