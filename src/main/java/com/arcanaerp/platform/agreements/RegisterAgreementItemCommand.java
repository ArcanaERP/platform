package com.arcanaerp.platform.agreements;

import java.util.UUID;

public record RegisterAgreementItemCommand(
    UUID agreementId,
    UUID agreementItemTypeId,
    String agreementItemValue,
    String description,
    String agreementItemRuleString
) {
}
