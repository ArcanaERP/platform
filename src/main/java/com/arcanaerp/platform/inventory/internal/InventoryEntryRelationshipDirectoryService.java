package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRelationshipCommand;
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
class InventoryEntryRelationshipDirectoryService implements InventoryEntryRelationshipDirectory {

    private final InventoryEntryRelationshipRepository relationshipRepository;
    private final InventoryEntryRelationshipTypeRepository relationshipTypeRepository;
    private final InventoryEntryRoleTypeRepository roleTypeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final Clock clock;

    @Override
    public InventoryEntryRelationshipView registerRelationship(RegisterInventoryEntryRelationshipCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String relationshipTypeCode = normalizeRequired(command.relationshipTypeCode(), "relationshipTypeCode").toUpperCase();
        String fromRoleTypeCode = normalizeRequired(command.fromRoleTypeCode(), "fromRoleTypeCode").toUpperCase();
        String toRoleTypeCode = normalizeRequired(command.toRoleTypeCode(), "toRoleTypeCode").toUpperCase();
        ensureRelationshipTypeExists(relationshipTypeCode);
        ensureRoleTypeExists(fromRoleTypeCode);
        ensureRoleTypeExists(toRoleTypeCode);
        InventoryItem fromItem = findItem(command.fromSku(), command.fromLocationCode(), "from");
        InventoryItem toItem = findItem(command.toSku(), command.toLocationCode(), "to");

        return toView(relationshipRepository.save(InventoryEntryRelationship.create(
            relationshipTypeCode,
            fromItem,
            toItem,
            fromRoleTypeCode,
            toRoleTypeCode,
            command.description(),
            command.statusCode(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryEntryRelationshipView relationshipById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toView(relationshipRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory entry relationship not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryEntryRelationshipView> listRelationships(
        String relationshipTypeCode,
        String fromSku,
        String fromLocationCode,
        String toSku,
        String toLocationCode,
        String statusCode,
        PageQuery pageQuery
    ) {
        Page<InventoryEntryRelationship> relationships = relationshipRepository.findRelationshipsFiltered(
            normalizeOptionalCode(relationshipTypeCode, "relationshipTypeCode"),
            normalizeOptionalCode(fromSku, "fromSku"),
            normalizeOptionalCode(fromLocationCode, "fromLocationCode"),
            normalizeOptionalCode(toSku, "toSku"),
            normalizeOptionalCode(toLocationCode, "toLocationCode"),
            normalizeOptionalCode(statusCode, "statusCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "relationshipTypeCode").and(Sort.by("fromSku")))
        );
        return PageResult.from(relationships).map(this::toView);
    }

    private void ensureRelationshipTypeExists(String relationshipTypeCode) {
        if (relationshipTypeRepository.findByCode(relationshipTypeCode).isEmpty()) {
            throw new IllegalArgumentException("Inventory entry relationship type not found: " + relationshipTypeCode);
        }
    }

    private void ensureRoleTypeExists(String roleTypeCode) {
        if (roleTypeRepository.findByCode(roleTypeCode).isEmpty()) {
            throw new IllegalArgumentException("Inventory entry role type not found: " + roleTypeCode);
        }
    }

    private InventoryItem findItem(String sku, String locationCode, String prefix) {
        String normalizedSku = normalizeRequired(sku, prefix + "Sku").toUpperCase();
        String normalizedLocationCode = normalizeRequired(locationCode, prefix + "LocationCode").toUpperCase();
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: " + normalizedSku + " at location: " + normalizedLocationCode
            ));
    }

    private InventoryEntryRelationshipView toView(InventoryEntryRelationship relationship) {
        return new InventoryEntryRelationshipView(
            relationship.getId(),
            relationship.getRelationshipTypeCode(),
            relationship.getFromSku(),
            relationship.getFromLocationCode(),
            relationship.getToSku(),
            relationship.getToLocationCode(),
            relationship.getFromRoleTypeCode(),
            relationship.getToRoleTypeCode(),
            relationship.getDescription(),
            relationship.getStatusCode(),
            relationship.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalCode(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toUpperCase();
    }
}
