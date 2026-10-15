package com.arcanaerp.platform.agreements.web;

import java.util.UUID;

public record CreateAgreementPartyRoleRequest(
    String description,
    UUID agreementId,
    Long partyId,
    Long roleTypeId,
    String externalIdentifier,
    String externalIdSource
) {
}
