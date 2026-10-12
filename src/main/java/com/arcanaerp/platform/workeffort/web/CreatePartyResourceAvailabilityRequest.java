package com.arcanaerp.platform.workeffort.web;

public record CreatePartyResourceAvailabilityRequest(
    String fromDate,
    String toDate,
    Long partyId,
    Long praTypeId
) {
}
