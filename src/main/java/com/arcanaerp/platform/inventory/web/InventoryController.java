package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryAvailability inventoryAvailability;

    @GetMapping("/{sku}")
    public InventoryItemResponse inventoryBySku(@PathVariable String sku) {
        InventoryItemView item = inventoryAvailability.inventoryForSku(sku);
        return new InventoryItemResponse(
            item.id(),
            item.sku(),
            item.onHandQuantity(),
            item.updatedAt()
        );
    }

    @PostMapping("/{sku}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAdjustmentResponse adjustInventory(
        @PathVariable String sku,
        @Valid @RequestBody AdjustInventoryRequest request
    ) {
        InventoryAdjustmentView adjustment = inventoryAvailability.adjustInventory(
            new AdjustInventoryCommand(
                sku,
                request.quantityDelta(),
                request.reason(),
                request.adjustedBy()
            )
        );
        return new InventoryAdjustmentResponse(
            adjustment.id(),
            adjustment.sku(),
            adjustment.previousOnHandQuantity(),
            adjustment.quantityDelta(),
            adjustment.currentOnHandQuantity(),
            adjustment.reason(),
            adjustment.adjustedBy(),
            adjustment.adjustedAt()
        );
    }
}
