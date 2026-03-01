package com.arcanaerp.platform.inventory;

import java.util.UUID;

public record ReverseInventoryTransferCommand(
    UUID transferId,
    String reason,
    String adjustedBy
) {
}
