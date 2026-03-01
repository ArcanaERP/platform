package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryAvailabilityService implements InventoryAvailability {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
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

    @Override
    public InventoryAdjustmentView adjustInventory(AdjustInventoryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        BigDecimal quantityDelta = normalizeQuantityDelta(command.quantityDelta());
        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();

        InventoryItem item = inventoryItemRepository.findBySku(normalizedSku)
            .orElseThrow(() -> new NoSuchElementException("Inventory item not found for SKU: " + normalizedSku));

        BigDecimal previousOnHand = item.getOnHandQuantity();
        Instant adjustedAt = Instant.now(clock);
        item.applyAdjustment(quantityDelta, adjustedAt);
        InventoryItem saved = inventoryItemRepository.save(item);

        InventoryAdjustment adjustment = inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                saved.getId(),
                saved.getSku(),
                previousOnHand,
                quantityDelta,
                saved.getOnHandQuantity(),
                reason,
                adjustedBy,
                adjustedAt
            )
        );

        return new InventoryAdjustmentView(
            adjustment.getId(),
            adjustment.getSku(),
            adjustment.getPreviousOnHandQuantity(),
            adjustment.getQuantityDelta(),
            adjustment.getCurrentOnHandQuantity(),
            adjustment.getReason(),
            adjustment.getAdjustedBy(),
            adjustment.getAdjustedAt()
        );
    }

    private static BigDecimal normalizeQuantityDelta(BigDecimal quantityDelta) {
        if (quantityDelta == null) {
            throw new IllegalArgumentException("quantityDelta is required");
        }
        if (quantityDelta.signum() == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        return quantityDelta;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
