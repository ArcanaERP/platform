package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgreementRelationshipView(
    UUID id,
    UUID agreementRelationshipTypeId,
    String description,
    UUID agreementIdFrom,
    UUID agreementIdTo,
    UUID roleTypeIdFrom,
    UUID roleTypeIdTo,
    Long statusTypeId,
    LocalDate fromDate,
    LocalDate thruDate,
    Instant createdAt
) {
}
