```
 _______                                 _______ ______  ______  
(_______)                               (_______|_____ \(_____ \
 _______  ____ ____ _____ ____  _____    _____   _____) )_____) )
|  ___  |/ ___) ___|____ |  _ \(____ |  |  ___) |  __  /|  ____/
| |   | | |  ( (___/ ___ | | | / ___ |  | |_____| |  \ \| |      
|_|   |_|_|   \____)_____|_| |_\_____|  |_______)_|   |_|_|v0.1.x

Arcana ERP v0.1.0-snapshot
(c) 2015 The Wizard & The Wyrd, LLC
rev. February 28, 2026
```

# ArcanaERP Platform

Java 21 + Spring Boot + Spring Modulith rewrite of Arcana ERP.

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Modulith 1.4.2
- Spring Data JPA + Hibernate
- H2 (dev/test)

## How To Run

Prerequisite: JDK 21 installed and available on `PATH`.

Build:

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Start the app:

```bash
./gradlew bootRun
```

Default app URL:

- `http://localhost:8080`

## H2 Console (Local Dev)

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:arcanaerp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: *(blank)*

## Operations

### Reversal Idempotency Runtime Knob

- Property: `arcanaerp.inventory.reversal-idempotency.pending-claim-ttl`
- Type: ISO-8601 duration (for example `PT5M`, `PT30S`, `PT1H`)
- Default: `PT5M`
- Validation: must be strictly positive (`PT0S` and negative durations fail startup)
- Purpose: determines when a pending reversal idempotency claim is considered stale and eligible for reclaim on retry.

### Recommended Values By Environment

| Environment | Recommended value | Why |
| --- | --- | --- |
| `dev` | `PT5M` | Good balance between retry safety and stale-claim recovery during local debugging. |
| `test` | `PT30S` (or lower for focused TTL tests) | Keeps automated stale-claim scenarios fast while still exercising recovery behavior. |
| `prod` | `PT5M` to `PT15M` | Reduces false stale reclaims during transient latency while avoiding long-lived stuck claims. |

### Full Property List

- See [docs/configuration-reference.md](docs/configuration-reference.md) for all runtime properties currently defined in main and test configs.

## Current HTTP Endpoints

Identity:

- `POST /api/identity/tenants`
- `GET /api/identity/tenants?page=&size=`
- `GET /api/identity/tenants/{code}`
- `PATCH /api/identity/tenants/{code}`
- `POST /api/identity/users`
- `POST /api/identity/roles`
- `GET /api/identity/users/{userId}`
- `GET /api/identity/users?page=&size=`
- `GET /api/identity/roles/{code}?tenantCode=`
- `PATCH /api/identity/roles/{code}?tenantCode=`
- `GET /api/identity/roles?tenantCode=&page=&size=`

Core:

- `POST /api/core/units-of-measurement`
- `GET /api/core/units-of-measurement?page=&size=&queryFilter=&domain=`

Products:

- `POST /api/products`
- `GET /api/products?page=&size=&active=`
- `PATCH /api/products/{sku}/active`
- `GET /api/products/{sku}/activation-history?page=&size=&tenantCode=&changedBy=&currentActive=&changedAtFrom=&changedAtTo=`

Orders:

- `POST /api/orders`
- `GET /api/orders?page=&size=`
- `PATCH /api/orders/{orderNumber}/status`

Invoicing:

- `POST /api/invoices`
- `GET /api/invoices/{invoiceNumber}`
- `GET /api/invoices?page=&size=&tenantCode=&status=&currencyCode=`
- `PATCH /api/invoices/{invoiceNumber}/status`
- `GET /api/invoices/{invoiceNumber}/status-history?page=&size=&previousStatus=&currentStatus=&changedAtFrom=&changedAtTo=`
- invoice responses currently include snapshot `lines[]` copied from the source order at creation time

Payments:

- `POST /api/payments`
- `GET /api/payments/invoices/{invoiceNumber}/balance`
- `GET /api/payments/tenants/{tenantCode}/receivables?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/summary?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging?currencyCode=`
- `GET /api/payments/tenants/{tenantCode}/receivables/aging/{agingBucket}?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90?currencyCode=&invoiceNumber=&assignedTo=&dueAtOnOrBefore=&followUpAtFrom=&followUpAtTo=&followUpScheduled=&latestFollowUpOutcome=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/assignee-summary?currencyCode=&assignedTo=&latestFollowUpOutcome=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-dashboard-summary?currencyCode=&assignedTo=&latestFollowUpOutcome=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-dashboard/daily-summary?assignedTo=&changedBy=&outcome=&changedAtFrom=&changedAtTo=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-dashboard/weekly-summary?assignedTo=&changedBy=&outcome=&changedAtFrom=&changedAtTo=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-dashboard/monthly-summary?assignedTo=&changedBy=&outcome=&changedAtFrom=&changedAtTo=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/unassigned?currencyCode=&latestFollowUpOutcome=&dueAtOnOrBefore=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/unassigned/summary?currencyCode=&latestFollowUpOutcome=`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/claim`
- `POST /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/release`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/claim-history?page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/release-history?page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/claim-history?page=&size=&invoiceNumber=&claimedBy=&claimedAtFrom=&claimedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/claims/daily-summary?page=&size=&claimedBy=&claimedAtFrom=&claimedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/claims/weekly-summary?page=&size=&claimedBy=&claimedAtFrom=&claimedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/claims/monthly-summary?page=&size=&claimedBy=&claimedAtFrom=&claimedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/release-history?page=&size=&invoiceNumber=&releasedBy=&releasedAtFrom=&releasedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/releases/daily-summary?page=&size=&releasedBy=&releasedAtFrom=&releasedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/releases/weekly-summary?page=&size=&releasedBy=&releasedAtFrom=&releasedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/releases/monthly-summary?page=&size=&releasedBy=&releasedAtFrom=&releasedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/net-intake/actor-summary?page=&size=&actor=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-operations-summary?currencyCode=&actor=&changedAtFrom=&changedAtTo=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-actor-effectiveness-summary?currencyCode=&assignedTo=&changedBy=&changedAtFrom=&changedAtTo=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-actor-effectiveness/daily-summary?currencyCode=&assignedTo=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-actor-effectiveness/weekly-summary?currencyCode=&assignedTo=&changedBy=&changedAtFrom=&changedAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/net-intake/daily-summary?page=&size=&actor=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/net-intake/weekly-summary?page=&size=&actor=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/net-intake/monthly-summary?page=&size=&actor=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome-summary?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/current-assignee-summary?currencyCode=&assignedTo=&latestFollowUpOutcome=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/assignee-summary?page=&size=&outcome=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/daily-summary?page=&size=&assignedTo=&outcome=&changedBy=&changedAtFrom=&changedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/actor/daily-summary?page=&size=&outcome=&changedBy=&changedAtFrom=&changedAtTo=&sortBy=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/actor/weekly-summary?page=&size=&outcome=&changedBy=&changedAtFrom=&changedAtTo=&sortBy=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/follow-up-outcome/actor/monthly-summary?page=&size=&outcome=&changedBy=&changedAtFrom=&changedAtTo=&sortBy=`
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
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignment-history?page=&size=&invoiceNumber=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/summary?currencyCode=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/assignee-aging-summary?currencyCode=&assignedTo=&agingBucket=&sortBy=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/daily-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/weekly-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments/tenants/{tenantCode}/receivables/collections/monthly-summary?page=&size=&assignedTo=&assignedAtFrom=&assignedAtTo=`
- `GET /api/payments?page=&size=&invoiceNumber=&tenantCode=&paidAtFrom=&paidAtTo=`
- `GET /api/payments/tenants/{tenantCode}/summary?currencyCode=&paidAtFrom=&paidAtTo=`
- `GET /api/payments/tenants/{tenantCode}/invoices?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/daily-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/monthly-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`
- `GET /api/payments/tenants/{tenantCode}/weekly-summary?currencyCode=&paidAtFrom=&paidAtTo=&page=&size=`

Agreements:

- `POST /api/agreements`
- `GET /api/agreements/{agreementNumber}`
- `GET /api/agreements?page=&size=&status=`
- `PATCH /api/agreements/{agreementNumber}/status` (request includes `status`, `tenantCode`, `reason`, `changedBy`)
- `GET /api/agreements/{agreementNumber}/status-history?page=&size=&tenantCode=&changedBy=&changedAtFrom=&changedAtTo=`

Inventory:

- `GET /api/inventory/{sku}?locationCode=` (`locationCode` defaults to `MAIN`)
- `POST /api/inventory/{sku}/adjustments?locationCode=` (`locationCode` defaults to `MAIN`)
- `POST /api/inventory/{sku}/transfers`
- `GET /api/inventory/transfers/{transferId}`
- `POST /api/inventory/transfers/{transferId}/reversals` (optional `Idempotency-Key` header for retry-safe replay; reusing a key with a different payload returns `409 Conflict`; concurrent first-write requests with the same key return `409 Conflict` for one request; stale pending claims are automatically reclaimed after 5 minutes on retry; duplicate reversal without a matching key also returns `409 Conflict`)
- Reversal stale-claim timeout is configurable via `arcanaerp.inventory.reversal-idempotency.pending-claim-ttl` (default `PT5M`).
- `GET /api/inventory/transfers/{transferId}/reversals?page=&size=`
- `GET /api/inventory/{sku}/transfers?page=&size=&sourceLocationCode=&destinationLocationCode=&adjustedBy=&referenceType=&referenceId=&adjustedAtFrom=&adjustedAtTo=`
- `GET /api/inventory/{sku}/adjustments?page=&size=&locationCode=&adjustedBy=&adjustedAtFrom=&adjustedAtTo=` (`locationCode` defaults to `MAIN`)

Actuator:

- `GET /actuator/health`
- `GET /actuator/info`

Actuator exposure is intentionally limited to `health` and `info`.
