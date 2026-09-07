package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementDirectory;
import com.arcanaerp.platform.identity.RoleDirectory;
import com.arcanaerp.platform.identity.UserDirectory;
import com.arcanaerp.platform.identity.UserView;
import com.arcanaerp.platform.inventory.InventoryItemDirectory;
import com.arcanaerp.platform.inventory.InventoryItemAvailabilityChangeView;
import com.arcanaerp.platform.inventory.InventoryItemMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryItemOwnerChangeView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.RegisterInventoryItemCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryItemAvailabilityCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryItemMetadataCommand;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
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
    private final InventoryItemOwnerChangeAuditRepository ownerChangeAuditRepository;
    private final InventoryItemAvailabilityChangeAuditRepository availabilityChangeAuditRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final UnitOfMeasurementDirectory unitOfMeasurementDirectory;
    private final UserDirectory userDirectory;
    private final RoleDirectory roleDirectory;
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
        BigDecimal availableQuantity = normalizeOptionalQuantity(
            command.availableQuantity(),
            "availableQuantity",
            onHandQuantity
        );
        BigDecimal soldQuantity = normalizeOptionalQuantity(command.soldQuantity(), "soldQuantity", BigDecimal.ZERO);
        if (availableQuantity.compareTo(onHandQuantity) > 0) {
            throw new IllegalArgumentException("availableQuantity must not exceed onHandQuantity");
        }
        ensureLocationActive(locationCode);
        if (inventoryItemRepository.findBySkuAndLocationCode(sku, locationCode).isPresent()) {
            throw new ConflictException("Inventory item already exists for SKU/location: " + sku + "/" + locationCode);
        }
        String unitOfMeasurementCode = normalizeOptionalCode(command.unitOfMeasurementCode(), "unitOfMeasurementCode", "EA");
        ensureUnitOfMeasurementExists(unitOfMeasurementCode);
        String externalReference = normalizeOptionalExternalReference(command.externalReference());
        String sourceSystemCode = normalizeOptionalCode(command.sourceSystemCode(), "sourceSystemCode");
        ensureExternalReferenceAvailable(sourceSystemCode, externalReference, null);
        OwnerAssignment ownerAssignment = resolveOwnerAssignment(
            command.ownerTenantCode(),
            command.ownerUserId(),
            command.ownerRoleCode()
        );

        return toView(inventoryItemRepository.save(InventoryItem.create(
            sku,
            locationCode,
            onHandQuantity,
            availableQuantity,
            soldQuantity,
            unitOfMeasurementCode,
            normalizeOptionalCode(command.classificationCode(), "classificationCode", "ON_HAND"),
            normalizeOptionalCode(command.productInstanceCode(), "productInstanceCode"),
            externalReference,
            sourceSystemCode,
            ownerAssignment.ownerTenantCode(),
            ownerAssignment.ownerUserId(),
            ownerAssignment.ownerRoleCode(),
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
        String previousProductInstanceCode = item.getProductInstanceCode();
        String previousExternalReference = item.getExternalReference();
        String previousSourceSystemCode = item.getSourceSystemCode();
        String previousOwnerTenantCode = item.getOwnerTenantCode();
        UUID previousOwnerUserId = item.getOwnerUserId();
        String previousOwnerRoleCode = item.getOwnerRoleCode();
        Instant changedAt = Instant.now(clock);
        ensureUnitOfMeasurementExists(command.unitOfMeasurementCode());
        String externalReference = normalizeOptionalExternalReference(command.externalReference());
        String sourceSystemCode = normalizeOptionalCode(command.sourceSystemCode(), "sourceSystemCode");
        ensureExternalReferenceAvailable(sourceSystemCode, externalReference, item.getId());
        OwnerAssignment ownerAssignment = resolveOwnerAssignment(
            command.ownerTenantCode(),
            command.ownerUserId(),
            command.ownerRoleCode()
        );
        item.updateMetadata(
            command.unitOfMeasurementCode(),
            command.classificationCode(),
            command.productInstanceCode(),
            externalReference,
            sourceSystemCode,
            ownerAssignment.ownerTenantCode(),
            ownerAssignment.ownerUserId(),
            ownerAssignment.ownerRoleCode(),
            changedAt
        );
        String changedBy = normalizeRequired(command.changedBy(), "changedBy").toLowerCase();
        if (ownerChanged(previousOwnerTenantCode, item.getOwnerTenantCode(), previousOwnerUserId, item.getOwnerUserId(), previousOwnerRoleCode, item.getOwnerRoleCode())) {
            ownerChangeAuditRepository.save(InventoryItemOwnerChangeAudit.create(
                item.getId(),
                item.getSku(),
                item.getLocationCode(),
                previousOwnerTenantCode,
                item.getOwnerTenantCode(),
                previousOwnerUserId,
                item.getOwnerUserId(),
                previousOwnerRoleCode,
                item.getOwnerRoleCode(),
                changedBy,
                changedAt
            ));
        }
        metadataChangeAuditRepository.save(InventoryItemMetadataChangeAudit.create(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
            previousUnitOfMeasurementCode,
            item.getUnitOfMeasurementCode(),
            previousClassificationCode,
            item.getClassificationCode(),
            previousProductInstanceCode,
            item.getProductInstanceCode(),
            previousExternalReference,
            item.getExternalReference(),
            previousSourceSystemCode,
            item.getSourceSystemCode(),
            previousOwnerTenantCode,
            item.getOwnerTenantCode(),
            previousOwnerUserId,
            item.getOwnerUserId(),
            previousOwnerRoleCode,
            item.getOwnerRoleCode(),
            changedBy,
            changedAt
        ));
        return toView(inventoryItemRepository.save(item));
    }

    @Override
    public InventoryItemView updateItemAvailability(
        String sku,
        String locationCode,
        UpdateInventoryItemAvailabilityCommand command
    ) {
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

        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeRequired(command.changedBy(), "changedBy").toLowerCase();
        InventoryItem item = findItem(normalizedSku, normalizedLocationCode);
        BigDecimal previousAvailableQuantity = item.getAvailableQuantity();
        BigDecimal previousSoldQuantity = item.getSoldQuantity();
        Instant changedAt = Instant.now(clock);
        item.applyAvailabilityChange(
            command.availableQuantityDelta(),
            command.soldQuantityDelta(),
            changedAt
        );
        availabilityChangeAuditRepository.save(InventoryItemAvailabilityChangeAudit.create(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
            previousAvailableQuantity,
            item.getAvailableQuantity(),
            command.availableQuantityDelta(),
            previousSoldQuantity,
            item.getSoldQuantity(),
            command.soldQuantityDelta(),
            reason,
            changedBy,
            changedAt
        ));
        return toView(inventoryItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemAvailabilityChangeView> listAvailabilityHistory(
        String sku,
        String locationCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItem item = findItem(sku, locationCode);
        Page<InventoryItemAvailabilityChangeAudit> history = availabilityChangeAuditRepository.findHistoryFiltered(
            item.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toAvailabilityChangeView);
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
    public PageResult<InventoryItemOwnerChangeView> listOwnerHistory(
        String sku,
        String locationCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItem item = findItem(sku, locationCode);
        Page<InventoryItemOwnerChangeAudit> history = ownerChangeAuditRepository.findHistoryFiltered(
            item.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toOwnerChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemView> listItems(
        String sku,
        String locationCode,
        String unitOfMeasurementCode,
        String classificationCode,
        String productInstanceCode,
        String externalReference,
        String sourceSystemCode,
        String ownerTenantCode,
        String ownerUserId,
        String ownerRoleCode,
        PageQuery pageQuery
    ) {
        Page<InventoryItem> items = inventoryItemRepository.findItemsFiltered(
            normalizeOptionalCode(sku, "sku"),
            normalizeOptionalCode(locationCode, "locationCode"),
            normalizeOptionalCode(unitOfMeasurementCode, "unitOfMeasurementCode"),
            normalizeOptionalCode(classificationCode, "classificationCode"),
            normalizeOptionalCode(productInstanceCode, "productInstanceCode"),
            normalizeOptionalExternalReference(externalReference),
            normalizeOptionalCode(sourceSystemCode, "sourceSystemCode"),
            normalizeOptionalCode(ownerTenantCode, "ownerTenantCode"),
            parseOptionalUuid(ownerUserId, "ownerUserId"),
            normalizeOptionalCode(ownerRoleCode, "ownerRoleCode"),
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
            item.getAvailableQuantity(),
            item.getSoldQuantity(),
            item.getUnitOfMeasurementCode(),
            item.getClassificationCode(),
            item.getProductInstanceCode(),
            item.getExternalReference(),
            item.getSourceSystemCode(),
            item.getOwnerTenantCode(),
            item.getOwnerUserId(),
            item.getOwnerRoleCode(),
            item.getUpdatedAt()
        );
    }

    private InventoryItemAvailabilityChangeView toAvailabilityChangeView(InventoryItemAvailabilityChangeAudit audit) {
        return new InventoryItemAvailabilityChangeView(
            audit.getId(),
            audit.getSku(),
            audit.getLocationCode(),
            audit.getPreviousAvailableQuantity(),
            audit.getCurrentAvailableQuantity(),
            audit.getAvailableQuantityDelta(),
            audit.getPreviousSoldQuantity(),
            audit.getCurrentSoldQuantity(),
            audit.getSoldQuantityDelta(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
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
            audit.getPreviousProductInstanceCode(),
            audit.getCurrentProductInstanceCode(),
            audit.getPreviousExternalReference(),
            audit.getCurrentExternalReference(),
            audit.getPreviousSourceSystemCode(),
            audit.getCurrentSourceSystemCode(),
            audit.getPreviousOwnerTenantCode(),
            audit.getCurrentOwnerTenantCode(),
            audit.getPreviousOwnerUserId(),
            audit.getCurrentOwnerUserId(),
            audit.getPreviousOwnerRoleCode(),
            audit.getCurrentOwnerRoleCode(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryItemOwnerChangeView toOwnerChangeView(InventoryItemOwnerChangeAudit audit) {
        return new InventoryItemOwnerChangeView(
            audit.getId(),
            audit.getSku(),
            audit.getLocationCode(),
            audit.getPreviousOwnerTenantCode(),
            audit.getCurrentOwnerTenantCode(),
            audit.getPreviousOwnerUserId(),
            audit.getCurrentOwnerUserId(),
            audit.getPreviousOwnerRoleCode(),
            audit.getCurrentOwnerRoleCode(),
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

    private static String normalizeOptionalExternalReference(String value) {
        return value == null ? null : normalizeRequired(value, "externalReference");
    }

    private void ensureExternalReferenceAvailable(String sourceSystemCode, String externalReference, UUID currentItemId) {
        if (sourceSystemCode == null || externalReference == null) {
            return;
        }
        inventoryItemRepository.findBySourceSystemCodeAndExternalReference(sourceSystemCode, externalReference)
            .filter(item -> currentItemId == null || !item.getId().equals(currentItemId))
            .ifPresent(item -> {
                throw new ConflictException(
                    "Inventory item external reference already exists for source system: "
                        + sourceSystemCode
                        + "/"
                        + externalReference
                );
            });
    }

    private OwnerAssignment resolveOwnerAssignment(String ownerTenantCode, String ownerUserId, String ownerRoleCode) {
        boolean hasTenant = ownerTenantCode != null && !ownerTenantCode.isBlank();
        boolean hasUser = ownerUserId != null && !ownerUserId.isBlank();
        boolean hasRole = ownerRoleCode != null && !ownerRoleCode.isBlank();
        if (!hasTenant && !hasUser && !hasRole) {
            return new OwnerAssignment(null, null, null);
        }
        if (!hasTenant || !hasUser || !hasRole) {
            throw new IllegalArgumentException("ownerTenantCode, ownerUserId, and ownerRoleCode must be supplied together");
        }

        String normalizedTenantCode = normalizeRequired(ownerTenantCode, "ownerTenantCode").toUpperCase();
        UUID normalizedUserId = parseRequiredUuid(ownerUserId, "ownerUserId");
        String normalizedRoleCode = normalizeRequired(ownerRoleCode, "ownerRoleCode").toUpperCase();
        roleDirectory.roleByCode(normalizedTenantCode, normalizedRoleCode);
        UserView user = userDirectory.userById(normalizedUserId.toString());
        if (!normalizedTenantCode.equals(user.tenantCode())) {
            throw new IllegalArgumentException("ownerUserId does not belong to ownerTenantCode");
        }
        if (!normalizedRoleCode.equals(user.roleCode())) {
            throw new IllegalArgumentException("ownerUserId does not have ownerRoleCode");
        }
        if (!user.active()) {
            throw new IllegalArgumentException("Owner user is inactive: " + normalizedUserId);
        }
        return new OwnerAssignment(normalizedTenantCode, normalizedUserId, normalizedRoleCode);
    }

    private static UUID parseOptionalUuid(String value, String fieldName) {
        return value == null ? null : parseRequiredUuid(value, fieldName);
    }

    private static UUID parseRequiredUuid(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName);
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID", exception);
        }
    }

    private static boolean ownerChanged(
        String previousOwnerTenantCode,
        String currentOwnerTenantCode,
        UUID previousOwnerUserId,
        UUID currentOwnerUserId,
        String previousOwnerRoleCode,
        String currentOwnerRoleCode
    ) {
        return !equalsNullable(previousOwnerTenantCode, currentOwnerTenantCode)
            || !equalsNullable(previousOwnerUserId, currentOwnerUserId)
            || !equalsNullable(previousOwnerRoleCode, currentOwnerRoleCode);
    }

    private static boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }

    private static BigDecimal normalizeOptionalQuantity(BigDecimal value, String fieldName, BigDecimal defaultValue) {
        BigDecimal quantity = value == null ? defaultValue : value;
        if (quantity == null || quantity.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or greater");
        }
        return quantity;
    }

    private record OwnerAssignment(String ownerTenantCode, UUID ownerUserId, String ownerRoleCode) {
    }
}
