package com.arcanaerp.platform.workeffort;

public record RegisterPartySkillCommand(
    Long partyId,
    Long skillTypeId
) {
}
