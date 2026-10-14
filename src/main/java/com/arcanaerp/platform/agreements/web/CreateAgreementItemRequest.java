package com.arcanaerp.platform.agreements.web;

import java.util.UUID;

public record CreateAgreementItemRequest(
    UUID agreementId,
    UUID agreementItemTypeId,
    String agreementItemValue,
    String description,
    String agreementItemRuleString
) {
}
