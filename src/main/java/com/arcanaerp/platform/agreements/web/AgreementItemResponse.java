package com.arcanaerp.platform.agreements.web;

import java.time.Instant;
import java.util.UUID;

public record AgreementItemResponse(
    UUID id,
    UUID agreementId,
    UUID agreementItemTypeId,
    String agreementItemValue,
    String description,
    String agreementItemRuleString,
    Instant createdAt
) {
}
