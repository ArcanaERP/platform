package com.arcanaerp.platform.communicationevents.web;

import jakarta.validation.constraints.NotBlank;

public record CreateCommunicationEventPurposeTypeRequest(
    @NotBlank String tenantCode,
    @NotBlank String code,
    @NotBlank String name
) {
}
