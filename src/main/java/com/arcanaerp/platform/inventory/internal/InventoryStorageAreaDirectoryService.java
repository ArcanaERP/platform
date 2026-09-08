package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryStorageAreaDirectory;
import com.arcanaerp.platform.inventory.InventoryStorageAreaView;
import com.arcanaerp.platform.inventory.RegisterInventoryStorageAreaCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryStorageAreaDirectoryService implements InventoryStorageAreaDirectory {

    private final InventoryStorageAreaRepository inventoryStorageAreaRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final Clock clock;

    @Override
    public InventoryStorageAreaView registerStorageArea(RegisterInventoryStorageAreaCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String facilityCode = normalizeRequired(command.facilityCode(), "facilityCode").toUpperCase();
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String parentStorageAreaCode = normalizeOptionalUpper(command.parentStorageAreaCode());
        InventoryStorageArea.normalizeStorageAreaType(command.storageAreaType());
        ensureFacilityExistsAndActive(facilityCode);
        ensureParentStorageAreaExists(facilityCode, code, parentStorageAreaCode);
        if (inventoryStorageAreaRepository.findByFacilityCodeAndCode(facilityCode, code).isPresent()) {
            throw new ConflictException(
                "Inventory storage area already exists for facility: " + facilityCode + " and code: " + code
            );
        }
        return toView(inventoryStorageAreaRepository.save(InventoryStorageArea.create(
            facilityCode,
            code,
            command.name(),
            command.storageAreaType(),
            parentStorageAreaCode,
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStorageAreaView storageAreaById(UUID id) {
        return toView(findStorageArea(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryStorageAreaView> listStorageAreas(
        String facilityCode,
        String storageAreaType,
        String parentStorageAreaCode,
        PageQuery pageQuery
    ) {
        return PageResult.from(inventoryStorageAreaRepository.findFiltered(
            normalizeOptionalUpper(facilityCode),
            normalizeOptionalStorageAreaType(storageAreaType),
            normalizeOptionalUpper(parentStorageAreaCode),
            pageQuery.toPageable(
                Sort.by(Sort.Direction.ASC, "facilityCode")
                    .and(Sort.by("parentStorageAreaCode"))
                    .and(Sort.by("code"))
            )
        )).map(this::toView);
    }

    private InventoryStorageArea findStorageArea(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return inventoryStorageAreaRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory storage area not found for id: " + id));
    }

    private void ensureFacilityExistsAndActive(String facilityCode) {
        InventoryFacility facility = inventoryFacilityRepository.findByCode(facilityCode)
            .orElseThrow(() -> new IllegalArgumentException("Inventory facility not found: " + facilityCode));
        if (!facility.isActive()) {
            throw new IllegalArgumentException("Inventory facility is inactive: " + facilityCode);
        }
    }

    private void ensureParentStorageAreaExists(String facilityCode, String code, String parentStorageAreaCode) {
        if (parentStorageAreaCode == null) {
            return;
        }
        if (code.equals(parentStorageAreaCode)) {
            throw new IllegalArgumentException("parentStorageAreaCode must not match code");
        }
        if (inventoryStorageAreaRepository.findByFacilityCodeAndCode(facilityCode, parentStorageAreaCode).isEmpty()) {
            throw new IllegalArgumentException(
                "Inventory storage area not found for facility: " + facilityCode + " and code: " + parentStorageAreaCode
            );
        }
    }

    private InventoryStorageAreaView toView(InventoryStorageArea storageArea) {
        return new InventoryStorageAreaView(
            storageArea.getId(),
            storageArea.getFacilityCode(),
            storageArea.getCode(),
            storageArea.getName(),
            storageArea.getStorageAreaType(),
            storageArea.getParentStorageAreaCode(),
            storageArea.getCreatedAt(),
            storageArea.getUpdatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("query parameter must not be blank");
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeOptionalStorageAreaType(String value) {
        return value == null ? null : InventoryStorageArea.normalizeStorageAreaType(value);
    }
}
