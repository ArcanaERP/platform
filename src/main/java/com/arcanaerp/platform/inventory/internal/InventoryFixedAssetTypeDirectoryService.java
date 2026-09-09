package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetTypeMetadataCommand;
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
class InventoryFixedAssetTypeDirectoryService implements InventoryFixedAssetTypeDirectory {

    private final InventoryFixedAssetTypeRepository inventoryFixedAssetTypeRepository;
    private final InventoryFixedAssetTypeMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final Clock clock;

    @Override
    public InventoryFixedAssetTypeView registerFixedAssetType(RegisterInventoryFixedAssetTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (inventoryFixedAssetTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory fixed asset type already exists for code: " + code);
        }
        return toView(inventoryFixedAssetTypeRepository.save(
            InventoryFixedAssetType.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFixedAssetTypeView fixedAssetTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(findFixedAssetType(normalizedCode));
    }

    @Override
    public InventoryFixedAssetTypeView updateFixedAssetTypeMetadata(
        String code,
        UpdateInventoryFixedAssetTypeMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryFixedAssetType type = findFixedAssetType(normalizedCode);
        InventoryFixedAssetTypeMetadataSnapshot previous = InventoryFixedAssetTypeMetadataSnapshot.from(type);
        Instant changedAt = Instant.now(clock);
        type.updateMetadata(command.description(), changedAt);
        metadataChangeAuditRepository.save(InventoryFixedAssetTypeMetadataChangeAudit.create(
            type,
            previous,
            InventoryFixedAssetTypeMetadataSnapshot.from(type),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryFixedAssetTypeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean fixedAssetTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryFixedAssetTypeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAssetType type = findFixedAssetType(code);
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
    public PageResult<InventoryFixedAssetTypeView> listFixedAssetTypes(PageQuery pageQuery) {
        return PageResult.from(
            inventoryFixedAssetTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryFixedAssetTypeView toView(InventoryFixedAssetType type) {
        return new InventoryFixedAssetTypeView(
            type.getId(),
            type.getCode(),
            type.getDescription(),
            type.getCreatedAt(),
            type.getUpdatedAt()
        );
    }

    private InventoryFixedAssetTypeMetadataChangeView toMetadataChangeView(
        InventoryFixedAssetTypeMetadataChangeAudit audit
    ) {
        return new InventoryFixedAssetTypeMetadataChangeView(
            audit.getId(),
            audit.getCode(),
            audit.getPreviousDescription(),
            audit.getCurrentDescription(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryFixedAssetType findFixedAssetType(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryFixedAssetTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset type not found for code: " + normalizedCode
            ));
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
