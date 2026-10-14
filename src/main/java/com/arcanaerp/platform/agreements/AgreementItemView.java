package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record AgreementItemView(
    UUID id,
    UUID agreementId,
    UUID agreementItemTypeId,
    String agreementItemValue,
    String description,
    String agreementItemRuleString,
    Instant createdAt
) {
}
