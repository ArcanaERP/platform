package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetMetadataCommand;
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
class InventoryFixedAssetDirectoryService implements InventoryFixedAssetDirectory {

    private final InventoryFixedAssetRepository inventoryFixedAssetRepository;
    private final InventoryFixedAssetActiveChangeAuditRepository activeChangeAuditRepository;
    private final InventoryFixedAssetMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final InventoryFixedAssetTypeDirectory inventoryFixedAssetTypeDirectory;
    private final Clock clock;

    @Override
    public InventoryFixedAssetView registerFixedAsset(RegisterInventoryFixedAssetCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String fixedAssetTypeCode = normalizeOptionalUpper(command.fixedAssetTypeCode());
        if (inventoryFixedAssetRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory fixed asset already exists for code: " + code);
        }
        ensureOptionalFixedAssetTypeExists(fixedAssetTypeCode);
        return toView(inventoryFixedAssetRepository.save(InventoryFixedAsset.create(
            code,
            command.description(),
            fixedAssetTypeCode,
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
    public InventoryFixedAssetView updateFixedAssetActive(String code, UpdateInventoryFixedAssetActiveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryFixedAsset fixedAsset = findFixedAsset(normalizedCode);
        boolean previousActive = fixedAsset.isActive();
        Instant changedAt = Instant.now(clock);
        fixedAsset.setActive(command.active(), changedAt);
        activeChangeAuditRepository.save(InventoryFixedAssetActiveChangeAudit.create(
            fixedAsset.getId(),
            fixedAsset.getCode(),
            previousActive,
            fixedAsset.isActive(),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryFixedAssetRepository.save(fixedAsset));
    }

    @Override
    public InventoryFixedAssetView updateFixedAssetMetadata(String code, UpdateInventoryFixedAssetMetadataCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        String fixedAssetTypeCode = normalizeOptionalUpper(command.fixedAssetTypeCode());
        ensureOptionalFixedAssetTypeExists(fixedAssetTypeCode);
        InventoryFixedAsset fixedAsset = findFixedAsset(normalizedCode);
        InventoryFixedAssetMetadataSnapshot previous = InventoryFixedAssetMetadataSnapshot.from(fixedAsset);
        Instant changedAt = Instant.now(clock);
        fixedAsset.updateMetadata(
            command.description(),
            fixedAssetTypeCode,
            command.comments(),
            command.externalIdentifier(),
            command.externalIdSource(),
            changedAt
        );
        metadataChangeAuditRepository.save(InventoryFixedAssetMetadataChangeAudit.create(
            fixedAsset.getId(),
            fixedAsset.getCode(),
            previous,
            InventoryFixedAssetMetadataSnapshot.from(fixedAsset),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryFixedAssetRepository.save(fixedAsset));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetActiveChangeView> listActiveHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAsset fixedAsset = findFixedAsset(code);
        Page<InventoryFixedAssetActiveChangeAudit> history = activeChangeAuditRepository.findHistoryFiltered(
            fixedAsset.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toActiveChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAsset fixedAsset = findFixedAsset(code);
        Page<InventoryFixedAssetMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            fixedAsset.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
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

    private InventoryFixedAsset findFixedAsset(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryFixedAssetRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory fixed asset not found for code: " + normalizedCode));
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

    private InventoryFixedAssetActiveChangeView toActiveChangeView(InventoryFixedAssetActiveChangeAudit audit) {
        return new InventoryFixedAssetActiveChangeView(
            audit.getId(),
            audit.getFixedAssetCode(),
            audit.isPreviousActive(),
            audit.isCurrentActive(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryFixedAssetMetadataChangeView toMetadataChangeView(InventoryFixedAssetMetadataChangeAudit audit) {
        return new InventoryFixedAssetMetadataChangeView(
            audit.getId(),
            audit.getFixedAssetCode(),
            audit.getPreviousDescription(),
            audit.getCurrentDescription(),
            audit.getPreviousFixedAssetTypeCode(),
            audit.getCurrentFixedAssetTypeCode(),
            audit.getPreviousComments(),
            audit.getCurrentComments(),
            audit.getPreviousExternalIdentifier(),
            audit.getCurrentExternalIdentifier(),
            audit.getPreviousExternalIdSource(),
            audit.getCurrentExternalIdSource(),
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

    private static String normalizeOptionalQuery(String query) {
        if (query == null) {
            return null;
        }
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        return query.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }

    private void ensureOptionalFixedAssetTypeExists(String fixedAssetTypeCode) {
        if (fixedAssetTypeCode == null) {
            return;
        }
        if (!inventoryFixedAssetTypeDirectory.fixedAssetTypeExists(fixedAssetTypeCode)) {
            throw new NoSuchElementException("Inventory fixed asset type not found for code: " + fixedAssetTypeCode);
        }
    }

    private static String normalizeOptionalUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
