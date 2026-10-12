package com.arcanaerp.platform.workeffort.web;

public record CreatePartySkillRequest(
    Long partyId,
    Long skillTypeId
) {
}
