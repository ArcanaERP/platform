package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryLocationTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationTypeMetadataCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryLocationTypeDirectoryService implements InventoryLocationTypeDirectory {

    private final InventoryLocationTypeRepository inventoryLocationTypeRepository;
    private final InventoryLocationTypeMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final Clock clock;

    @Override
    public InventoryLocationTypeView registerLocationType(RegisterInventoryLocationTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        String parentCode = normalizeOptionalUpper(command.parentCode());
        ensureParentExists(code, parentCode);
        if (inventoryLocationTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory location type already exists for code: " + code);
        }
        return toView(inventoryLocationTypeRepository.save(
            InventoryLocationType.create(code, description, parentCode, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLocationTypeView locationTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(findLocationType(normalizedCode));
    }

    @Override
    public InventoryLocationTypeView updateLocationTypeMetadata(
        String code,
        UpdateInventoryLocationTypeMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryLocationType type = findLocationType(normalizedCode);
        String parentCode = normalizeOptionalUpper(command.parentCode());
        ensureParentExists(normalizedCode, parentCode);
        ensureParentChangeDoesNotCreateCycle(normalizedCode, parentCode);
        InventoryLocationTypeMetadataSnapshot previous = InventoryLocationTypeMetadataSnapshot.from(type);
        Instant changedAt = Instant.now(clock);
        type.updateMetadata(command.description(), parentCode, changedAt);
        metadataChangeAuditRepository.save(InventoryLocationTypeMetadataChangeAudit.create(
            type,
            previous,
            InventoryLocationTypeMetadataSnapshot.from(type),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryLocationTypeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean locationTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryLocationTypeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryLocationTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryLocationType type = findLocationType(code);
        return PageResult.from(metadataChangeAuditRepository.findHistoryFiltered(
            type.getCode(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        )).map(this::toMetadataChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryLocationTypeView> listLocationTypes(String parentCode, PageQuery pageQuery) {
        String normalizedParentCode = normalizeOptionalUpper(parentCode);
        if (normalizedParentCode != null) {
            return PageResult.from(
                inventoryLocationTypeRepository.findByParentCode(
                    normalizedParentCode,
                    pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code"))
                )
            ).map(this::toView);
        }
        return PageResult.from(
            inventoryLocationTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryLocationTypeView toView(InventoryLocationType type) {
        return new InventoryLocationTypeView(
            type.getId(),
            type.getCode(),
            type.getDescription(),
            type.getParentCode(),
            type.getCreatedAt(),
            type.getUpdatedAt()
        );
    }

    private InventoryLocationTypeMetadataChangeView toMetadataChangeView(
        InventoryLocationTypeMetadataChangeAudit audit
    ) {
        return new InventoryLocationTypeMetadataChangeView(
            audit.getId(),
            audit.getCode(),
            audit.getPreviousDescription(),
            audit.getCurrentDescription(),
            audit.getPreviousParentCode(),
            audit.getCurrentParentCode(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryLocationType findLocationType(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryLocationTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory location type not found for code: " + normalizedCode
            ));
    }

    private void ensureParentExists(String code, String parentCode) {
        if (parentCode == null) {
            return;
        }
        if (code.equals(parentCode)) {
            throw new IllegalArgumentException("parentCode must not match code");
        }
        if (inventoryLocationTypeRepository.findByCode(parentCode).isEmpty()) {
            throw new IllegalArgumentException("Inventory location type not found for parentCode: " + parentCode);
        }
    }

    private void ensureParentChangeDoesNotCreateCycle(String code, String parentCode) {
        String currentParentCode = parentCode;
        while (currentParentCode != null) {
            if (code.equals(currentParentCode)) {
                throw new IllegalArgumentException("parentCode must not create a cycle");
            }
            currentParentCode = inventoryLocationTypeRepository
                .findByCode(currentParentCode)
                .map(InventoryLocationType::getParentCode)
                .orElse(null);
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
