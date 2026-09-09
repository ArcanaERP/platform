package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetFacilityAssignmentTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand;
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
class InventoryFixedAssetFacilityAssignmentTypeDirectoryService
    implements InventoryFixedAssetFacilityAssignmentTypeDirectory {

    private final InventoryFixedAssetFacilityAssignmentTypeRepository assignmentTypeRepository;
    private final InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final Clock clock;

    @Override
    public InventoryFixedAssetFacilityAssignmentTypeView registerAssignmentType(
        RegisterInventoryFixedAssetFacilityAssignmentTypeCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (assignmentTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException(
                "Inventory fixed asset facility assignment type already exists for code: " + code
            );
        }
        return toView(assignmentTypeRepository.save(
            InventoryFixedAssetFacilityAssignmentType.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFixedAssetFacilityAssignmentTypeView assignmentTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(findAssignmentType(normalizedCode));
    }

    @Override
    public InventoryFixedAssetFacilityAssignmentTypeView updateAssignmentTypeMetadata(
        String code,
        UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryFixedAssetFacilityAssignmentType type = findAssignmentType(normalizedCode);
        InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot previous =
            InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot.from(type);
        Instant changedAt = Instant.now(clock);
        type.updateMetadata(command.description(), changedAt);
        metadataChangeAuditRepository.save(InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit.create(
            type,
            previous,
            InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot.from(type),
            command.changedBy(),
            changedAt
        ));
        return toView(assignmentTypeRepository.save(type));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean assignmentTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return assignmentTypeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAssetFacilityAssignmentType type = findAssignmentType(code);
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
    public PageResult<InventoryFixedAssetFacilityAssignmentTypeView> listAssignmentTypes(PageQuery pageQuery) {
        return PageResult.from(
            assignmentTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryFixedAssetFacilityAssignmentTypeView toView(InventoryFixedAssetFacilityAssignmentType type) {
        return new InventoryFixedAssetFacilityAssignmentTypeView(
            type.getId(),
            type.getCode(),
            type.getDescription(),
            type.getCreatedAt(),
            type.getUpdatedAt()
        );
    }

    private InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView toMetadataChangeView(
        InventoryFixedAssetFacilityAssignmentTypeMetadataChangeAudit audit
    ) {
        return new InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView(
            audit.getId(),
            audit.getCode(),
            audit.getPreviousDescription(),
            audit.getCurrentDescription(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryFixedAssetFacilityAssignmentType findAssignmentType(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return assignmentTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset facility assignment type not found for code: " + normalizedCode
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
