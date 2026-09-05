package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementDirectory;
import com.arcanaerp.platform.inventory.InventoryItemDirectory;
import com.arcanaerp.platform.inventory.InventoryItemMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.RegisterInventoryItemCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryItemMetadataCommand;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryItemDirectoryService implements InventoryItemDirectory {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryItemMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final UnitOfMeasurementDirectory unitOfMeasurementDirectory;
    private final Clock clock;

    @Override
    public InventoryItemView registerItem(RegisterInventoryItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String sku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String locationCode = normalizeRequired(command.locationCode(), "locationCode").toUpperCase();
        BigDecimal onHandQuantity = command.onHandQuantity();
        if (onHandQuantity == null || onHandQuantity.signum() < 0) {
            throw new IllegalArgumentException("onHandQuantity must be zero or greater");
        }
        ensureLocationActive(locationCode);
        if (inventoryItemRepository.findBySkuAndLocationCode(sku, locationCode).isPresent()) {
            throw new ConflictException("Inventory item already exists for SKU/location: " + sku + "/" + locationCode);
        }
        String unitOfMeasurementCode = normalizeOptionalCode(command.unitOfMeasurementCode(), "unitOfMeasurementCode", "EA");
        ensureUnitOfMeasurementExists(unitOfMeasurementCode);

        return toView(inventoryItemRepository.save(InventoryItem.create(
            sku,
            locationCode,
            onHandQuantity,
            unitOfMeasurementCode,
            normalizeOptionalCode(command.classificationCode(), "classificationCode", "ON_HAND"),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemView itemBySkuAndLocation(String sku, String locationCode) {
        return toView(findItem(sku, locationCode));
    }

    @Override
    public InventoryItemView updateItemMetadata(String sku, String locationCode, UpdateInventoryItemMetadataCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedLocationCode = normalizeRequired(locationCode, "locationCode").toUpperCase();
        String commandSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String commandLocationCode = normalizeRequired(command.locationCode(), "locationCode").toUpperCase();
        if (!normalizedSku.equals(commandSku)) {
            throw new IllegalArgumentException("sku path variable must match command sku");
        }
        if (!normalizedLocationCode.equals(commandLocationCode)) {
            throw new IllegalArgumentException("locationCode path variable must match command locationCode");
        }
        InventoryItem item = findItem(normalizedSku, normalizedLocationCode);
        String previousUnitOfMeasurementCode = item.getUnitOfMeasurementCode();
        String previousClassificationCode = item.getClassificationCode();
        Instant changedAt = Instant.now(clock);
        ensureUnitOfMeasurementExists(command.unitOfMeasurementCode());
        item.updateMetadata(command.unitOfMeasurementCode(), command.classificationCode(), changedAt);
        String changedBy = normalizeRequired(command.changedBy(), "changedBy").toLowerCase();
        metadataChangeAuditRepository.save(InventoryItemMetadataChangeAudit.create(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
            previousUnitOfMeasurementCode,
            item.getUnitOfMeasurementCode(),
            previousClassificationCode,
            item.getClassificationCode(),
            changedBy,
            changedAt
        ));
        return toView(inventoryItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemMetadataChangeView> listMetadataHistory(
        String sku,
        String locationCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItem item = findItem(sku, locationCode);
        Page<InventoryItemMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            item.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemView> listItems(
        String sku,
        String locationCode,
        String unitOfMeasurementCode,
        String classificationCode,
        PageQuery pageQuery
    ) {
        Page<InventoryItem> items = inventoryItemRepository.findItemsFiltered(
            normalizeOptionalCode(sku, "sku"),
            normalizeOptionalCode(locationCode, "locationCode"),
            normalizeOptionalCode(unitOfMeasurementCode, "unitOfMeasurementCode"),
            normalizeOptionalCode(classificationCode, "classificationCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "sku").and(Sort.by(Sort.Direction.ASC, "locationCode")))
        );
        return PageResult.from(items).map(this::toView);
    }

    private InventoryItem findItem(String sku, String locationCode) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedLocationCode = normalizeRequired(locationCode, "locationCode").toUpperCase();
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: " + normalizedSku + " at location: " + normalizedLocationCode
            ));
    }

    private void ensureLocationActive(String locationCode) {
        InventoryLocation location = inventoryLocationRepository.findByCode(locationCode)
            .orElseGet(() -> inventoryLocationRepository.save(
                InventoryLocation.create(locationCode, locationCode, Instant.now(clock))
            ));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Inventory location is inactive: " + locationCode);
        }
    }

    private void ensureUnitOfMeasurementExists(String unitOfMeasurementCode) {
        String normalizedUnitOfMeasurementCode = normalizeRequired(unitOfMeasurementCode, "unitOfMeasurementCode").toUpperCase();
        if (!unitOfMeasurementDirectory.unitOfMeasurementExists(normalizedUnitOfMeasurementCode)) {
            throw new IllegalArgumentException("Unit of measurement not found: " + normalizedUnitOfMeasurementCode);
        }
    }

    private InventoryItemView toView(InventoryItem item) {
        return new InventoryItemView(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
            item.getOnHandQuantity(),
            item.getUnitOfMeasurementCode(),
            item.getClassificationCode(),
            item.getUpdatedAt()
        );
    }

    private InventoryItemMetadataChangeView toMetadataChangeView(InventoryItemMetadataChangeAudit audit) {
        return new InventoryItemMetadataChangeView(
            audit.getId(),
            audit.getSku(),
            audit.getLocationCode(),
            audit.getPreviousUnitOfMeasurementCode(),
            audit.getCurrentUnitOfMeasurementCode(),
            audit.getPreviousClassificationCode(),
            audit.getCurrentClassificationCode(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalCode(String value, String fieldName, String defaultValue) {
        return value == null ? defaultValue : normalizeRequired(value, fieldName).toUpperCase();
    }

    private static String normalizeOptionalCode(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
