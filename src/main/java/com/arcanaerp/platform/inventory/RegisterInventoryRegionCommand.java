package com.arcanaerp.platform.inventory;

public record RegisterInventoryRegionCommand(
    String countryCode,
    String code,
    String name
) {
}
