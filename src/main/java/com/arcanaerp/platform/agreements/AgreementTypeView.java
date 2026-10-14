package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record AgreementTypeView(
    UUID id,
    Long parentId,
    Integer left,
    Integer right,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
