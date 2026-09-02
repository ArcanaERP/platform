# Work Effort Data Model

Updated: 2026-08-30

## Scope

Current work-effort slice covers tenant-scoped work-effort registration, direct lookup, filtered listing, lightweight status transitions, assignment changes, append-only status history, append-only assignment history, assignment activity summaries, and status activity summaries.

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
- create, status-transition, and assignment commands validate actor emails through the `identity` module
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
- status activity summaries bucket status transitions by UTC `changedAt`; weekly buckets start on Monday
- status activity summary rows include `transitionCount` and distinct `workEffortCount`
- status activity summaries require `tenantCode` and support optional `previousStatus`, `currentStatus`, `changedBy`, `changedAtFrom`, and `changedAtTo` filters

### WorkEffortAssignmentChangeAudit

Purpose:
- keep assignment changes append-only and queryable for audit/history views

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `effortNumber`
- `previousAssignedTo`
- `currentAssignedTo`
- `tenantCode`
- `reason`
- `assignedBy`
- `assignedAt`

Rules:
- each successful assignee change appends one audit row
- no-op assignment changes return the current work effort without appending history
- history reads are newest-first by `assignedAt`
- optional history filters support exact `tenantCode`, exact current `assignedTo`, exact `assignedBy`, and `assignedAtFrom` / `assignedAtTo`

## Cross-Module Dependency

- `workeffort` validates assignees, status actors, and assignment actors through public `IdentityActorLookup`
- no dependency on `identity.internal`

## Minimal HTTP Surface

- `POST /api/work-efforts`
- `GET /api/work-efforts/{effortNumber}?tenantCode=`
- `GET /api/work-efforts?tenantCode=&status=&assignedTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity-summary?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/daily-summary?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/daily-summary/by-assignee?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/weekly-summary?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/weekly-summary/by-assignee?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/monthly-summary?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/assignment-activity/monthly-summary/by-assignee?tenantCode=&assignedTo=&assignedAtFrom=&assignedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/daily-summary?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/daily-summary/by-current-status?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/weekly-summary?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/weekly-summary/by-current-status?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/monthly-summary?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/status-activity/monthly-summary/by-current-status?tenantCode=&previousStatus=&currentStatus=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `PATCH /api/work-efforts/{effortNumber}/status` (request includes `tenantCode`, `status`, `reason`, `changedBy`)
- `GET /api/work-efforts/{effortNumber}/status-history?tenantCode=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/work-efforts/{effortNumber}/assignment?tenantCode=`
- `PATCH /api/work-efforts/{effortNumber}/assignment` (request includes `tenantCode`, `assignedTo`, `reason`, `assignedBy`)
- `GET /api/work-efforts/{effortNumber}/assignment-history?tenantCode=&assignedTo=&assignedBy=&assignedAtFrom=&assignedAtTo=&page=&size=`

## Query Notes

- work-effort listing is paged through the shared `PageQuery` contract
- blank query values are rejected at the HTTP boundary
- status-history ranges require `changedAtFrom <= changedAtTo`
- assignment-history ranges require `assignedAtFrom <= assignedAtTo`
- assignment activity summaries are grouped by current assignee from immutable assignment-change audits; daily/weekly/monthly by-assignee endpoints add the same assignee split inside each UTC bucket
- daily, weekly, and monthly assignment activity summaries use UTC bucket boundaries
- daily, weekly, and monthly status activity summaries use UTC bucket boundaries
