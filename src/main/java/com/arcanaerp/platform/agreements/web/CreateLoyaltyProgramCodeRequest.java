package com.arcanaerp.platform.agreements.web;

public record CreateLoyaltyProgramCodeRequest(
    String identifier,
    String name,
    String description
) {
}
