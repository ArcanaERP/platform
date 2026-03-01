package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryAvailabilityService implements InventoryAvailability {

    private static final String DEFAULT_LOCATION_CODE = "MAIN";

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public InventoryItemView inventoryForSku(String sku, String locationCode) {
        InventoryItem item = findInventoryItem(sku, locationCode);

        return new InventoryItemView(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
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
        String normalizedLocationCode = normalizeLocationCode(command.locationCode());
        BigDecimal quantityDelta = normalizeQuantityDelta(command.quantityDelta());
        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();
        ensureLocationExists(normalizedLocationCode);

        InventoryItem item = findInventoryItem(normalizedSku, normalizedLocationCode);

        BigDecimal previousOnHand = item.getOnHandQuantity();
        Instant adjustedAt = Instant.now(clock);
        item.applyAdjustment(quantityDelta, adjustedAt);
        InventoryItem saved = inventoryItemRepository.save(item);

        InventoryAdjustment adjustment = inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                saved.getId(),
                saved.getSku(),
                saved.getLocationCode(),
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
            adjustment.getLocationCode(),
            adjustment.getPreviousOnHandQuantity(),
            adjustment.getQuantityDelta(),
            adjustment.getCurrentOnHandQuantity(),
            adjustment.getReason(),
            adjustment.getAdjustedBy(),
            adjustment.getAdjustedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryAdjustmentView> listAdjustments(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItem item = findInventoryItem(sku, locationCode);
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        PageRequest pageRequest = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "adjustedAt"));

        Page<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findHistoryFiltered(
            item.getId(),
            normalizedAdjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageRequest
        );

        return PageResult.from(adjustments).map(adjustment -> new InventoryAdjustmentView(
                adjustment.getId(),
                adjustment.getSku(),
                adjustment.getLocationCode(),
                adjustment.getPreviousOnHandQuantity(),
                adjustment.getQuantityDelta(),
                adjustment.getCurrentOnHandQuantity(),
                adjustment.getReason(),
                adjustment.getAdjustedBy(),
                adjustment.getAdjustedAt()
            ));
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

    private static String normalizeLocationCode(String locationCode) {
        if (locationCode == null) {
            return DEFAULT_LOCATION_CODE;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException("locationCode is required");
        }
        return locationCode.trim().toUpperCase();
    }

    private InventoryItem findInventoryItem(String sku, String locationCode) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedLocationCode = normalizeLocationCode(locationCode);
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: " + normalizedSku + " at location: " + normalizedLocationCode
            ));
    }

    private void ensureLocationExists(String locationCode) {
        inventoryLocationRepository.findByCode(locationCode)
            .orElseGet(() -> inventoryLocationRepository.save(
                InventoryLocation.create(locationCode, locationCode, Instant.now(clock))
            ));
    }
}
