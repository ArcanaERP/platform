package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.InventoryTransferView;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
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
                null,
                previousOnHand,
                quantityDelta,
                saved.getOnHandQuantity(),
                reason,
                adjustedBy,
                null,
                null,
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
    public InventoryTransferView transferInventory(TransferInventoryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String sourceLocationCode = normalizeRequired(command.sourceLocationCode(), "sourceLocationCode").toUpperCase();
        String destinationLocationCode = normalizeRequired(command.destinationLocationCode(), "destinationLocationCode").toUpperCase();
        if (sourceLocationCode.equals(destinationLocationCode)) {
            throw new IllegalArgumentException("sourceLocationCode and destinationLocationCode must be different");
        }
        BigDecimal quantity = normalizePositiveQuantity(command.quantity());
        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();
        String referenceType = normalizeOptionalReferenceType(command.referenceType());
        String referenceId = normalizeOptionalReferenceId(command.referenceId());
        validateReferencePair(referenceType, referenceId);

        ensureLocationExists(sourceLocationCode);
        ensureLocationExists(destinationLocationCode);

        InventoryItem sourceItem = findInventoryItem(normalizedSku, sourceLocationCode);
        Instant adjustedAt = Instant.now(clock);
        InventoryItem destinationItem = inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, destinationLocationCode)
            .orElseGet(() -> InventoryItem.create(normalizedSku, destinationLocationCode, BigDecimal.ZERO, adjustedAt));

        BigDecimal sourcePreviousOnHand = sourceItem.getOnHandQuantity();
        BigDecimal destinationPreviousOnHand = destinationItem.getOnHandQuantity();
        sourceItem.applyAdjustment(quantity.negate(), adjustedAt);
        destinationItem.applyAdjustment(quantity, adjustedAt);

        InventoryItem savedSource = inventoryItemRepository.save(sourceItem);
        InventoryItem savedDestination = inventoryItemRepository.save(destinationItem);

        UUID transferId = UUID.randomUUID();
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                savedSource.getId(),
                savedSource.getSku(),
                savedSource.getLocationCode(),
                transferId,
                sourcePreviousOnHand,
                quantity.negate(),
                savedSource.getOnHandQuantity(),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                adjustedAt
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                savedDestination.getId(),
                savedDestination.getSku(),
                savedDestination.getLocationCode(),
                transferId,
                destinationPreviousOnHand,
                quantity,
                savedDestination.getOnHandQuantity(),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                adjustedAt
            )
        );

        return new InventoryTransferView(
            transferId,
            normalizedSku,
            sourceLocationCode,
            destinationLocationCode,
            quantity,
            savedSource.getOnHandQuantity(),
            savedDestination.getOnHandQuantity(),
            reason,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryTransferView> listTransfers(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedSourceLocationCode = normalizeOptionalLocationCodeFilter(sourceLocationCode, "sourceLocationCode");
        String normalizedDestinationLocationCode = normalizeOptionalLocationCodeFilter(
            destinationLocationCode,
            "destinationLocationCode"
        );
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        String normalizedReferenceType = normalizeOptionalReferenceType(referenceType);
        String normalizedReferenceId = normalizeOptionalReferenceId(referenceId);

        Page<InventoryAdjustmentRepository.TransferHistoryProjection> transfers = inventoryAdjustmentRepository.findTransferHistoryFiltered(
            normalizedSku,
            normalizedSourceLocationCode,
            normalizedDestinationLocationCode,
            normalizedAdjustedBy,
            normalizedReferenceType,
            normalizedReferenceId,
            adjustedAtFrom,
            adjustedAtTo,
            PageRequest.of(pageQuery.page(), pageQuery.size())
        );

        return PageResult.from(transfers).map(transfer -> new InventoryTransferView(
                transfer.getTransferId(),
                transfer.getSku(),
                transfer.getSourceLocationCode(),
                transfer.getDestinationLocationCode(),
                transfer.getSourceQuantityDelta().abs(),
                transfer.getSourceOnHandQuantity(),
                transfer.getDestinationOnHandQuantity(),
                transfer.getReason(),
                transfer.getAdjustedBy(),
                transfer.getReferenceType(),
                transfer.getReferenceId(),
                transfer.getTransferredAt()
            ));
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

    private static BigDecimal normalizePositiveQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity is required");
        }
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return quantity;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalReferenceType(String referenceType) {
        if (referenceType == null) {
            return null;
        }
        if (referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required");
        }
        return referenceType.trim().toUpperCase();
    }

    private static String normalizeOptionalReferenceId(String referenceId) {
        if (referenceId == null) {
            return null;
        }
        if (referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId is required");
        }
        return referenceId.trim();
    }

    private static void validateReferencePair(String referenceType, String referenceId) {
        if ((referenceType == null) == (referenceId == null)) {
            return;
        }
        throw new IllegalArgumentException("referenceType and referenceId must both be provided together");
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

    private static String normalizeOptionalLocationCodeFilter(String locationCode, String fieldName) {
        if (locationCode == null) {
            return null;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
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

    private void ensureSkuExists(String sku) {
        if (!inventoryItemRepository.existsBySku(sku)) {
            throw new NoSuchElementException("Inventory item not found for SKU: " + sku);
        }
    }

    private void ensureLocationExists(String locationCode) {
        inventoryLocationRepository.findByCode(locationCode)
            .orElseGet(() -> inventoryLocationRepository.save(
                InventoryLocation.create(locationCode, locationCode, Instant.now(clock))
            ));
    }
}
