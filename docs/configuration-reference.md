# ArcanaERP Platform Configuration Reference

Updated: 2026-03-01

This document lists runtime properties currently defined by the platform and their defaults in:

- `src/main/resources/application.properties`
- `src/test/resources/application.properties`

## Application

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `spring.application.name` | `platform` | *(inherits framework default)* | Application identifier for logs/metadata. |

## DataSource (H2)

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `spring.datasource.url` | `jdbc:h2:mem:arcanaerp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | `jdbc:h2:mem:arcanaerp_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | In-memory DB URL for local runtime/tests. |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | `org.h2.Driver` | JDBC driver. |
| `spring.datasource.username` | `sa` | `sa` | DB username. |
| `spring.datasource.password` | *(blank)* | *(blank)* | DB password. |
| `spring.h2.console.enabled` | `true` | *(not set)* | Enables local H2 web console. |

## JPA/Hibernate

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `spring.jpa.hibernate.ddl-auto` | `update` | `create-drop` | Auto-DDL strategy (`update` for local iteration, `create-drop` for test isolation). |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `UTC` | `UTC` | JDBC timezone consistency. |
| `spring.jpa.open-in-view` | `false` | `false` | Disables Open Session in View. |

## Inventory Idempotency

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `arcanaerp.inventory.reversal-idempotency.pending-claim-ttl` | `PT5M` | `PT5M` | TTL for pending reversal-idempotency claims before stale-claim recovery can reclaim them. Must be positive. |

## Management/Actuator

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `management.endpoints.web.exposure.include` | `health,info` | `health,info` | Exposed actuator endpoints. |
| `management.endpoints.web.discovery.enabled` | `false` | `false` | Disables actuator discovery page. |
| `management.info.env.enabled` | `true` | `true` | Includes environment info in `info` endpoint. |

## App Info Metadata

| Property | Main default | Test default | Purpose |
| --- | --- | --- | --- |
| `info.app.name` | `ArcanaERP Platform` | `ArcanaERP Platform` | Application info endpoint metadata. |
| `info.app.module` | `platform` | `platform` | Module identifier in info endpoint metadata. |

## Operational Notes

- Use environment-specific overrides (env vars, profile-specific properties, deployment config) instead of editing base property files for per-env tuning.
- For idempotency tuning, start with `PT5M` in production and adjust only with observed retry/latency data.
