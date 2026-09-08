package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryContactPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryContactPurposeView;
import com.arcanaerp.platform.inventory.RegisterInventoryContactPurposeCommand;
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
class InventoryContactPurposeDirectoryService implements InventoryContactPurposeDirectory {

    private final InventoryContactPurposeRepository inventoryContactPurposeRepository;
    private final Clock clock;

    @Override
    public InventoryContactPurposeView registerContactPurpose(RegisterInventoryContactPurposeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (inventoryContactPurposeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory contact purpose already exists for code: " + code);
        }
        return toView(inventoryContactPurposeRepository.save(
            InventoryContactPurpose.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryContactPurposeView contactPurposeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryContactPurposeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory contact purpose not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contactPurposeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryContactPurposeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryContactPurposeView> listContactPurposes(PageQuery pageQuery) {
        return PageResult.from(
            inventoryContactPurposeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryContactPurposeView toView(InventoryContactPurpose purpose) {
        return new InventoryContactPurposeView(
            purpose.getId(),
            purpose.getCode(),
            purpose.getDescription(),
            purpose.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
