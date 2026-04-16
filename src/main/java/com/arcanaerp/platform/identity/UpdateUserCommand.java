package com.arcanaerp.platform.identity;

public record UpdateUserCommand(
    String userId,
    String displayName,
    boolean active
) {
}
