package com.arcanaerp.platform.communicationevents;

public record RegisterCommunicationEventPurposeTypeCommand(
    String tenantCode,
    String code,
    String name
) {
}
