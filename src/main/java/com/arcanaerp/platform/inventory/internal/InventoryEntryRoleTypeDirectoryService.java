package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRoleTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRoleTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRoleTypeCommand;
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
class InventoryEntryRoleTypeDirectoryService implements InventoryEntryRoleTypeDirectory {

    private final InventoryEntryRoleTypeRepository roleTypeRepository;
    private final Clock clock;

    @Override
    public InventoryEntryRoleTypeView registerRoleType(RegisterInventoryEntryRoleTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (roleTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory entry role type already exists for code: " + code);
        }
        return toView(roleTypeRepository.save(
            InventoryEntryRoleType.create(code, description, command.comments(), Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryEntryRoleTypeView roleTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(roleTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory entry role type not found for code: " + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean roleTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return roleTypeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryEntryRoleTypeView> listRoleTypes(PageQuery pageQuery) {
        return PageResult.from(
            roleTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryEntryRoleTypeView toView(InventoryEntryRoleType type) {
        return new InventoryEntryRoleTypeView(
            type.getId(),
            type.getCode(),
            type.getDescription(),
            type.getComments(),
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
