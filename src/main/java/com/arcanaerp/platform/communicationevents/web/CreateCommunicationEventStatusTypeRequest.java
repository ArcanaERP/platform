package com.arcanaerp.platform.communicationevents.web;

import jakarta.validation.constraints.NotBlank;

public record CreateCommunicationEventStatusTypeRequest(
    @NotBlank String tenantCode,
    @NotBlank String code,
    @NotBlank String name
) {
}
