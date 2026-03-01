# Agreements Module Data Model (High-Level)

Updated: 2026-03-01

## Entity Diagram

```mermaid
erDiagram
    AGREEMENTS {
      UUID id PK
      STRING agreementNumber UK
      STRING name
      STRING agreementType
      STRING status
      INSTANT effectiveFrom
      INSTANT createdAt
    }
```

## Relationship Notes

- This initial `agreements` slice models a single aggregate (`Agreement`) with no external entity links.
- `agreementNumber` is the external business identifier and is normalized to uppercase.
- `agreementType` is stored as an uppercase normalized string for consistent filtering/parity expansion.
- `status` starts as `DRAFT` in this first slice.

## Constraint Notes

- Unique constraints:
  - `agreements(agreementNumber)`
