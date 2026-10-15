package com.arcanaerp.platform.agreements;

public record RegisterLoyaltyProgramCodeCommand(
    String identifier,
    String name,
    String description
) {
}
