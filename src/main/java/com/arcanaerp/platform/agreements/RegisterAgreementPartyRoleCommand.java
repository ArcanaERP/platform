package com.arcanaerp.platform.agreements;

import java.util.UUID;

public record RegisterAgreementPartyRoleCommand(
    String description,
    UUID agreementId,
    Long partyId,
    Long roleTypeId,
    String externalIdentifier,
    String externalIdSource
) {
}
