# Work Effort Data Model

Updated: 2026-08-30

## Scope

Current work-effort slice covers tenant-scoped work-effort registration, direct lookup, filtered listing, lightweight status transitions, assignment changes, append-only status history, append-only assignment history, assignment activity summaries, status activity summaries, legacy-compatible associated-record joins, legacy-compatible work-order item fulfillment joins, legacy-compatible order requirement commitments, legacy-compatible association types, legacy-compatible work-effort associations, legacy-compatible fixed-asset assignment joins, legacy-compatible inventory assignment joins, legacy-compatible party assignment joins, and legacy-compatible role-type assignment joins.

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

### AssociatedWorkEffort

Purpose:
- mirror legacy `associated_work_efforts` records linking a work effort to a polymorphic associated record
- support the legacy `has_many_polymorphic` association shape without adding target-module dependencies

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `associatedRecordId`
- `associatedRecordType`

Rules:
- writes require an existing work effort by `tenantCode + effortNumber`
- `tenantCode` and `effortNumber` are normalized to uppercase
- `associatedRecordType` is trimmed but preserves case because the legacy value is a Rails class/type discriminator
- duplicate rows are allowed for parity with the legacy non-unique indexes

### WorkOrderItemFulfillment

Purpose:
- mirror legacy `work_order_item_fulfillments` records linking work efforts to order line items
- track the work effort by which an order line item is fulfilled without adding Orders module behavior

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `orderLineItemId`
- `description`
- `createdAt`

Rules:
- writes require an existing work effort by `tenantCode + effortNumber`
- `tenantCode` and `effortNumber` are normalized to uppercase
- `orderLineItemId` is stored as a logical cross-module reference while Work Effort has no Orders dependency
- duplicate rows are allowed for parity with the legacy non-unique composite index

### OrderRequirementCommitment

Purpose:
- mirror legacy `order_requirement_commitments` records linking order line items to requirements
- track the requirement an order line item commits to without adding Orders or Requirement dependencies

Core fields:
- `id` (`UUID`)
- `orderLineItemId`
- `requirementId`
- `description`
- `quantity`
- `createdAt`

Rules:
- `orderLineItemId` and `requirementId` are required
- `orderLineItemId` and `requirementId` are stored as logical cross-module references while Work Effort has no Orders or Requirement dependency
- duplicate rows are allowed for parity with the legacy non-unique composite index

### WorkEffortAssociationType

Purpose:
- mirror legacy `work_effort_association_types` records for dependency, concurrence, and breakdown association classifications
- retain role-type metadata without adding a direct Role catalog dependency

Core fields:
- `id` (`UUID`)
- `code`
- `name`
- `description`
- `parentTypeCode`
- `validFromRoleTypeCode`
- `validToRoleTypeCode`
- `externalIdentifier`
- `externalIdSource`
- `createdAt`

Rules:
- `code`, `parentTypeCode`, `validFromRoleTypeCode`, and `validToRoleTypeCode` are normalized to uppercase
- `code` is unique in the Java slice as the stable replacement for the legacy internal identifier
- parent and role-type references are logical code references in this slice

### WorkEffortAssociation

Purpose:
- mirror legacy `work_effort_associations` records connecting two work efforts
- support precedence, concurrence, and breakdown links without adding graph traversal behavior not present in the current Java slice

Core fields:
- `id` (`UUID`)
- `tenantCode`
- `associationTypeCode`
- `description`
- `fromWorkEffortId`
- `fromEffortNumber`
- `toWorkEffortId`
- `toEffortNumber`
- `fromRoleTypeCode`
- `toRoleTypeCode`
- `relationshipTypeCode`
- `effectiveFrom`
- `effectiveThru`
- `createdAt`

Rules:
- writes require an existing association type and existing from/to work efforts in the same tenant
- `tenantCode`, work-effort numbers, association type, role-type references, and relationship type are normalized to uppercase
- `effectiveFrom` and `effectiveThru` are optional, but `effectiveFrom` must be before or equal to `effectiveThru` when both are present
- duplicate rows are allowed for parity with the legacy table, which has no unique association constraint

### WorkEffortFixedAssetAssignment

Purpose:
- mirror legacy `work_effort_fixed_asset_assignments` join records between work efforts and fixed assets
- track fixed assets used to execute a work effort without adding lifecycle behavior not present in the legacy table

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `fixedAssetCode`
- `createdAt`

Rules:
- `tenantCode`, `effortNumber`, and `fixedAssetCode` are normalized to uppercase
- writes require an existing work effort by `tenantCode + effortNumber`
- `fixedAssetCode` is a logical cross-module reference while Work Effort has no Inventory dependency
- duplicate `workEffort + fixedAsset` assignment rows are allowed for parity with the legacy non-unique index

### WorkEffortInventoryAssignment

Purpose:
- mirror legacy `work_effort_inventory_assignments` join records between work efforts and inventory entries
- track inventory entries used in the execution of a work effort without adding lifecycle behavior not present in the legacy table

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `inventoryEntryCode`
- `createdAt`

Rules:
- `tenantCode`, `effortNumber`, and `inventoryEntryCode` are normalized to uppercase
- writes require an existing work effort by `tenantCode + effortNumber`
- `inventoryEntryCode` is a logical cross-module reference while Work Effort has no Inventory dependency
- duplicate `workEffort + inventoryEntry` assignment rows are allowed for parity with the legacy non-unique index

### WorkEffortPartyAssignment

Purpose:
- mirror legacy `work_effort_party_assignments` records linking work efforts to parties and role types
- represent business-party assignment to a work effort without adding actor validation or end-history behavior not present in the legacy table

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `partyCode`
- `roleTypeCode`
- `assignedFrom`
- `assignedThru`
- `comments`
- `createdAt`

Rules:
- `tenantCode`, `effortNumber`, `partyCode`, and `roleTypeCode` are normalized to uppercase
- writes require an existing work effort by `tenantCode + effortNumber`
- `assignedFrom` and `assignedThru` are optional, but `assignedFrom` must be before or equal to `assignedThru` when both are present
- `partyCode` and `roleTypeCode` are logical cross-module references while Work Effort has no direct Party/Role catalog dependency
- duplicate rows are allowed for parity with the legacy non-unique indexes

### WorkEffortRoleTypeAssignment

Purpose:
- mirror legacy `role_types_work_efforts` HABTM join records between work efforts and role types
- represent role-type assignment to a work effort without adding fields not present in the legacy table

Core fields:
- `id` (`UUID`)
- `workEffortId`
- `tenantCode`
- `effortNumber`
- `roleTypeCode`

Rules:
- `tenantCode`, `effortNumber`, and `roleTypeCode` are normalized to uppercase
- writes require an existing work effort by `tenantCode + effortNumber`
- `roleTypeCode` is a logical cross-module reference while Work Effort has no direct Role catalog dependency
- duplicate rows are allowed for parity with the legacy non-unique composite index

## Cross-Module Dependency

- `workeffort` validates assignees, status actors, and assignment actors through public `IdentityActorLookup`
- fixed-asset assignment records store fixed-asset codes as logical cross-module references; this slice does not add an Inventory dependency
- inventory assignment records store inventory-entry codes as logical cross-module references; this slice does not add an Inventory dependency
- party assignment records store party and role-type codes as logical cross-module references; this slice does not add a Party or Role catalog dependency
- role-type assignment records store role-type codes as logical cross-module references; this slice does not add a Role catalog dependency
- association type records store role-type references as logical codes; this slice does not add a Role catalog dependency
- association records store relationship-type references as logical codes; this slice does not add a Relationship Type catalog dependency
- associated-record links store polymorphic record id/type values without depending on target modules
- work-order item fulfillments store order-line-item ids as logical references without depending on Orders
- order requirement commitments store order-line-item and requirement ids as logical references without depending on Orders or Requirement
- no dependency on `identity.internal`

## Minimal HTTP Surface

- `POST /api/work-efforts`
- `POST /api/work-efforts/associated-records`
- `GET /api/work-efforts/associated-records/{id}`
- `GET /api/work-efforts/associated-records?tenantCode=&effortNumber=&associatedRecordId=&associatedRecordType=&page=&size=`
- `POST /api/work-efforts/work-order-item-fulfillments`
- `GET /api/work-efforts/work-order-item-fulfillments/{id}`
- `GET /api/work-efforts/work-order-item-fulfillments?tenantCode=&effortNumber=&orderLineItemId=&page=&size=`
- `POST /api/work-efforts/order-requirement-commitments`
- `GET /api/work-efforts/order-requirement-commitments/{id}`
- `GET /api/work-efforts/order-requirement-commitments?orderLineItemId=&requirementId=&page=&size=`
- `POST /api/work-efforts/association-types`
- `GET /api/work-efforts/association-types/{code}`
- `GET /api/work-efforts/association-types?parentTypeCode=&page=&size=`
- `POST /api/work-efforts/associations`
- `GET /api/work-efforts/associations/{id}`
- `GET /api/work-efforts/associations?tenantCode=&associationTypeCode=&fromEffortNumber=&toEffortNumber=&relationshipTypeCode=&effectiveFrom=&effectiveThru=&page=&size=`
- `POST /api/work-efforts/fixed-asset-assignments`
- `GET /api/work-efforts/fixed-asset-assignments/{id}`
- `GET /api/work-efforts/fixed-asset-assignments?tenantCode=&effortNumber=&fixedAssetCode=&page=&size=`
- `POST /api/work-efforts/inventory-assignments`
- `GET /api/work-efforts/inventory-assignments/{id}`
- `GET /api/work-efforts/inventory-assignments?tenantCode=&effortNumber=&inventoryEntryCode=&page=&size=`
- `POST /api/work-efforts/party-assignments`
- `GET /api/work-efforts/party-assignments/{id}`
- `GET /api/work-efforts/party-assignments?tenantCode=&effortNumber=&partyCode=&roleTypeCode=&assignedFrom=&assignedThru=&page=&size=`
- `POST /api/work-efforts/role-type-assignments`
- `GET /api/work-efforts/role-type-assignments/{id}`
- `GET /api/work-efforts/role-type-assignments?tenantCode=&effortNumber=&roleTypeCode=&page=&size=`
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
- associated work-effort listing supports optional exact `tenantCode`, `effortNumber`, `associatedRecordId`, and `associatedRecordType` filters
- work-order item fulfillment listing supports optional exact `tenantCode`, `effortNumber`, and `orderLineItemId` filters
- order requirement commitment listing supports optional exact `orderLineItemId` and `requirementId` filters
- work-effort association type listing supports optional exact `parentTypeCode` filtering
- work-effort association listing supports optional exact tenant, association type, from effort, to effort, and relationship type filters plus optional effective date bounds
- work-effort fixed-asset assignment listing supports optional exact `tenantCode`, `effortNumber`, and `fixedAssetCode` filters
- work-effort inventory assignment listing supports optional exact `tenantCode`, `effortNumber`, and `inventoryEntryCode` filters
- work-effort party assignment listing supports optional exact `tenantCode`, `effortNumber`, `partyCode`, and `roleTypeCode` filters plus optional assignment date bounds
- work-effort role-type assignment listing supports optional exact `tenantCode`, `effortNumber`, and `roleTypeCode` filters
- blank query values are rejected at the HTTP boundary
- status-history ranges require `changedAtFrom <= changedAtTo`
- assignment-history ranges require `assignedAtFrom <= assignedAtTo`
- assignment activity summaries are grouped by current assignee from immutable assignment-change audits; daily/weekly/monthly by-assignee endpoints add the same assignee split inside each UTC bucket
- daily, weekly, and monthly assignment activity summaries use UTC bucket boundaries
- daily, weekly, and monthly status activity summaries use UTC bucket boundaries
