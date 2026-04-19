package com.arcanaerp.platform.rules.web;

import jakarta.validation.constraints.NotBlank;

public record CreateRuleDefinitionRequest(
    @NotBlank String tenantCode,
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String appliesTo,
    @NotBlank String expression,
    String description,
    boolean active
) {
}
