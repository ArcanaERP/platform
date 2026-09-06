package com.arcanaerp.platform.inventory;

public record RegisterInventoryEntryRoleTypeCommand(
    String code,
    String description,
    String comments
) {
}
