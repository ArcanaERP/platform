package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetCommand;
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
class InventoryFixedAssetDirectoryService implements InventoryFixedAssetDirectory {

    private final InventoryFixedAssetRepository inventoryFixedAssetRepository;
    private final Clock clock;

    @Override
    public InventoryFixedAssetView registerFixedAsset(RegisterInventoryFixedAssetCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        if (inventoryFixedAssetRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory fixed asset already exists for code: " + code);
        }
        return toView(inventoryFixedAssetRepository.save(InventoryFixedAsset.create(
            code,
            command.description(),
            command.fixedAssetTypeCode(),
            command.comments(),
            command.externalIdentifier(),
            command.externalIdSource(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFixedAssetView fixedAssetByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryFixedAssetRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory fixed asset not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetView> listFixedAssets(Boolean active, String query, PageQuery pageQuery) {
        return PageResult.from(inventoryFixedAssetRepository.findFiltered(
            active,
            normalizeOptionalQuery(query),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code"))
        )).map(this::toView);
    }

    private InventoryFixedAssetView toView(InventoryFixedAsset fixedAsset) {
        return new InventoryFixedAssetView(
            fixedAsset.getId(),
            fixedAsset.getCode(),
            fixedAsset.getDescription(),
            fixedAsset.getFixedAssetTypeCode(),
            fixedAsset.getComments(),
            fixedAsset.getExternalIdentifier(),
            fixedAsset.getExternalIdSource(),
            fixedAsset.isActive(),
            fixedAsset.getCreatedAt(),
            fixedAsset.getUpdatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalQuery(String query) {
        if (query == null) {
            return null;
        }
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        return query.trim().toUpperCase();
    }
}
