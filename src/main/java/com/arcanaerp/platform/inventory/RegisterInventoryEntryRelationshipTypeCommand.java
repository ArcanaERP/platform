package com.arcanaerp.platform.inventory;

public record RegisterInventoryEntryRelationshipTypeCommand(
    String code,
    String description,
    String comments
) {
}
