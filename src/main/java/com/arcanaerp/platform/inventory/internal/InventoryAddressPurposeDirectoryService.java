package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryAddressPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryAddressPurposeView;
import com.arcanaerp.platform.inventory.RegisterInventoryAddressPurposeCommand;
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
class InventoryAddressPurposeDirectoryService implements InventoryAddressPurposeDirectory {

    private final InventoryAddressPurposeRepository inventoryAddressPurposeRepository;
    private final Clock clock;

    @Override
    public InventoryAddressPurposeView registerAddressPurpose(RegisterInventoryAddressPurposeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (inventoryAddressPurposeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory address purpose already exists for code: " + code);
        }
        return toView(inventoryAddressPurposeRepository.save(
            InventoryAddressPurpose.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAddressPurposeView addressPurposeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryAddressPurposeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory address purpose not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean addressPurposeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryAddressPurposeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryAddressPurposeView> listAddressPurposes(PageQuery pageQuery) {
        return PageResult.from(
            inventoryAddressPurposeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryAddressPurposeView toView(InventoryAddressPurpose purpose) {
        return new InventoryAddressPurposeView(
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
