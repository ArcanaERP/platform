package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortAssociationTypeView(
    UUID id,
    String code,
    String name,
    String description,
    String parentTypeCode,
    String validFromRoleTypeCode,
    String validToRoleTypeCode,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
