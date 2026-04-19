package com.arcanaerp.platform.rules;

public record RegisterRuleDefinitionCommand(
    String tenantCode,
    String code,
    String name,
    String appliesTo,
    String expression,
    String description,
    boolean active
) {
}
