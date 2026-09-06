package com.arcanaerp.platform.inventory;

public record RegisterInventoryProductInstanceAssignmentCommand(
    String sku,
    String locationCode,
    String productInstanceCode,
    String assignedBy
) {
}
