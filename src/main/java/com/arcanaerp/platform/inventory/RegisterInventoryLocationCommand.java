package com.arcanaerp.platform.inventory;

public record RegisterInventoryLocationCommand(
    String code,
    String name
) {
}
