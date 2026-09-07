package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetTypeCommand;
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
        return toView(inventoryFixedAssetTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory fixed asset type not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean fixedAssetTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryFixedAssetTypeRepository.findByCode(normalizedCode).isPresent();
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
            type.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
