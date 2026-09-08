package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPartyRoleTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyRoleTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyRoleTypeCommand;
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
class InventoryPartyRoleTypeDirectoryService implements InventoryPartyRoleTypeDirectory {

    private final InventoryPartyRoleTypeRepository roleTypeRepository;
    private final Clock clock;

    @Override
    public InventoryPartyRoleTypeView registerRoleType(RegisterInventoryPartyRoleTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (roleTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory party role type already exists for code: " + code);
        }
        return toView(roleTypeRepository.save(
            InventoryPartyRoleType.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPartyRoleTypeView roleTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(roleTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory party role type not found for code: " + normalizedCode
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
    public PageResult<InventoryPartyRoleTypeView> listRoleTypes(PageQuery pageQuery) {
        return PageResult.from(
            roleTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryPartyRoleTypeView toView(InventoryPartyRoleType type) {
        return new InventoryPartyRoleTypeView(
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
