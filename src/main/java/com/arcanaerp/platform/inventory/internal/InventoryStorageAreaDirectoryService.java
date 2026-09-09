package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryStorageAreaDirectory;
import com.arcanaerp.platform.inventory.InventoryStorageAreaActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryStorageAreaMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryStorageAreaView;
import com.arcanaerp.platform.inventory.RegisterInventoryStorageAreaCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryStorageAreaActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryStorageAreaMetadataCommand;
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
    private final InventoryStorageAreaActiveChangeAuditRepository activeChangeAuditRepository;
    private final InventoryStorageAreaMetadataChangeAuditRepository metadataChangeAuditRepository;
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
        ensureParentStorageAreaExists(facilityCode, code, parentStorageAreaCode, true);
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
    public InventoryStorageAreaView updateStorageAreaActive(
        UUID id,
        UpdateInventoryStorageAreaActiveCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryStorageArea storageArea = findStorageArea(id);
        if (
            !command.active()
                && inventoryStorageAreaRepository.existsByFacilityCodeAndParentStorageAreaCodeAndActiveTrue(
                    storageArea.getFacilityCode(),
                    storageArea.getCode()
                )
        ) {
            throw new IllegalArgumentException("Inventory storage area has active child storage areas");
        }
        boolean previousActive = storageArea.isActive();
        Instant changedAt = Instant.now(clock);
        storageArea.setActive(command.active(), changedAt);
        activeChangeAuditRepository.save(InventoryStorageAreaActiveChangeAudit.create(
            storageArea,
            previousActive,
            storageArea.isActive(),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryStorageAreaRepository.save(storageArea));
    }

    @Override
    public InventoryStorageAreaView updateStorageAreaMetadata(
        UUID id,
        UpdateInventoryStorageAreaMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryStorageArea storageArea = findStorageArea(id);
        String storageAreaType = InventoryStorageArea.normalizeStorageAreaType(command.storageAreaType());
        String parentStorageAreaCode = normalizeOptionalUpper(command.parentStorageAreaCode());
        ensureParentStorageAreaExists(
            storageArea.getFacilityCode(),
            storageArea.getCode(),
            parentStorageAreaCode,
            storageArea.isActive()
        );
        ensureParentChangeDoesNotCreateCycle(storageArea.getFacilityCode(), storageArea.getCode(), parentStorageAreaCode);
        InventoryStorageAreaMetadataSnapshot previous = InventoryStorageAreaMetadataSnapshot.from(storageArea);
        Instant changedAt = Instant.now(clock);
        storageArea.updateMetadata(
            command.name(),
            storageAreaType,
            parentStorageAreaCode,
            changedAt
        );
        metadataChangeAuditRepository.save(InventoryStorageAreaMetadataChangeAudit.create(
            storageArea,
            previous,
            InventoryStorageAreaMetadataSnapshot.from(storageArea),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryStorageAreaRepository.save(storageArea));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryStorageAreaActiveChangeView> listActiveHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryStorageArea storageArea = findStorageArea(id);
        return PageResult.from(activeChangeAuditRepository.findHistoryFiltered(
            storageArea.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        )).map(this::toActiveChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryStorageAreaMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryStorageArea storageArea = findStorageArea(id);
        return PageResult.from(metadataChangeAuditRepository.findHistoryFiltered(
            storageArea.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        )).map(this::toMetadataChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryStorageAreaView> listStorageAreas(
        Boolean active,
        String facilityCode,
        String storageAreaType,
        String parentStorageAreaCode,
        PageQuery pageQuery
    ) {
        return PageResult.from(inventoryStorageAreaRepository.findFiltered(
            active,
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

    private void ensureParentStorageAreaExists(
        String facilityCode,
        String code,
        String parentStorageAreaCode,
        boolean childWillBeActive
    ) {
        if (parentStorageAreaCode == null) {
            return;
        }
        if (code.equals(parentStorageAreaCode)) {
            throw new IllegalArgumentException("parentStorageAreaCode must not match code");
        }
        InventoryStorageArea parent = inventoryStorageAreaRepository
            .findByFacilityCodeAndCode(facilityCode, parentStorageAreaCode)
            .orElseThrow(() -> new IllegalArgumentException(
                "Inventory storage area not found for facility: " + facilityCode + " and code: " + parentStorageAreaCode
            ));
        if (childWillBeActive && !parent.isActive()) {
            throw new IllegalArgumentException(
                "parentStorageAreaCode must reference an active storage area: " + parentStorageAreaCode
            );
        }
    }

    private void ensureParentChangeDoesNotCreateCycle(
        String facilityCode,
        String storageAreaCode,
        String parentStorageAreaCode
    ) {
        String currentParentCode = parentStorageAreaCode;
        while (currentParentCode != null) {
            if (storageAreaCode.equals(currentParentCode)) {
                throw new IllegalArgumentException("parentStorageAreaCode must not create a cycle");
            }
            currentParentCode = inventoryStorageAreaRepository
                .findByFacilityCodeAndCode(facilityCode, currentParentCode)
                .map(InventoryStorageArea::getParentStorageAreaCode)
                .orElse(null);
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
            storageArea.isActive(),
            storageArea.getCreatedAt(),
            storageArea.getUpdatedAt()
        );
    }

    private InventoryStorageAreaActiveChangeView toActiveChangeView(InventoryStorageAreaActiveChangeAudit audit) {
        return new InventoryStorageAreaActiveChangeView(
            audit.getId(),
            audit.getStorageAreaId(),
            audit.getFacilityCode(),
            audit.getStorageAreaCode(),
            audit.isPreviousActive(),
            audit.isCurrentActive(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryStorageAreaMetadataChangeView toMetadataChangeView(
        InventoryStorageAreaMetadataChangeAudit audit
    ) {
        return new InventoryStorageAreaMetadataChangeView(
            audit.getId(),
            audit.getStorageAreaId(),
            audit.getFacilityCode(),
            audit.getStorageAreaCode(),
            audit.getPreviousName(),
            audit.getCurrentName(),
            audit.getPreviousStorageAreaType(),
            audit.getCurrentStorageAreaType(),
            audit.getPreviousParentStorageAreaCode(),
            audit.getCurrentParentStorageAreaCode(),
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

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
