package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryItemView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
