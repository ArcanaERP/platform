package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationTypeCommand;
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
        return toView(inventoryLocationTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location type not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean locationTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryLocationTypeRepository.findByCode(normalizedCode).isPresent();
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
            type.getCreatedAt()
        );
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
}
