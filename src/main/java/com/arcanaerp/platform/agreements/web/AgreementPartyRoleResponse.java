package com.arcanaerp.platform.agreements.web;

import java.time.Instant;
import java.util.UUID;

public record AgreementPartyRoleResponse(
    UUID id,
    String description,
    UUID agreementId,
    Long partyId,
    Long roleTypeId,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
