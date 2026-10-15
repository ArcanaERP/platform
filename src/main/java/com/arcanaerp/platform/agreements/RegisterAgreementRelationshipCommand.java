package com.arcanaerp.platform.agreements;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterAgreementRelationshipCommand(
    UUID agreementRelationshipTypeId,
    String description,
    UUID agreementIdFrom,
    UUID agreementIdTo,
    UUID roleTypeIdFrom,
    UUID roleTypeIdTo,
    Long statusTypeId,
    LocalDate fromDate,
    LocalDate thruDate
) {
}
