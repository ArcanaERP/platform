package com.arcanaerp.platform.communicationevents;

public record RegisterCommunicationEventStatusTypeCommand(
    String tenantCode,
    String code,
    String name
) {
}
