package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryItemView;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class InventoryAvailabilityService implements InventoryAvailability {

    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public InventoryItemView inventoryForSku(String sku) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        InventoryItem item = inventoryItemRepository.findBySku(normalizedSku)
            .orElseThrow(() -> new NoSuchElementException("Inventory item not found for SKU: " + normalizedSku));

        return new InventoryItemView(
            item.getId(),
            item.getSku(),
            item.getOnHandQuantity(),
            item.getUpdatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
