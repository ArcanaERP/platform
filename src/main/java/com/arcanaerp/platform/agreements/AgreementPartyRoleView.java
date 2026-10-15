package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record AgreementPartyRoleView(
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
