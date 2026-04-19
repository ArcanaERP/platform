package com.arcanaerp.platform.communicationevents;

public record ChangeCommunicationEventStatusCommand(
    String tenantCode,
    String eventNumber,
    String statusCode,
    String reason,
    String changedBy
) {
}
