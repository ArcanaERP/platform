package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetFacilityAssignmentTypeCommand;
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
        return toView(assignmentTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset facility assignment type not found for code: " + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean assignmentTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return assignmentTypeRepository.findByCode(normalizedCode).isPresent();
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
