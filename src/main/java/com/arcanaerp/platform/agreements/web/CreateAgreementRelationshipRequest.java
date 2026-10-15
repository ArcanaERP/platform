package com.arcanaerp.platform.agreements.web;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAgreementRelationshipRequest(
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
