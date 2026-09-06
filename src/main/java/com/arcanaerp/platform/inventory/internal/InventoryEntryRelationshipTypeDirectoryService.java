package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRelationshipTypeCommand;
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
class InventoryEntryRelationshipTypeDirectoryService implements InventoryEntryRelationshipTypeDirectory {

    private final InventoryEntryRelationshipTypeRepository relationshipTypeRepository;
    private final Clock clock;

    @Override
    public InventoryEntryRelationshipTypeView registerRelationshipType(
        RegisterInventoryEntryRelationshipTypeCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (relationshipTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory entry relationship type already exists for code: " + code);
        }
        return toView(relationshipTypeRepository.save(
            InventoryEntryRelationshipType.create(code, description, command.comments(), Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryEntryRelationshipTypeView relationshipTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(relationshipTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory entry relationship type not found for code: " + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean relationshipTypeExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return relationshipTypeRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryEntryRelationshipTypeView> listRelationshipTypes(PageQuery pageQuery) {
        return PageResult.from(
            relationshipTypeRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryEntryRelationshipTypeView toView(InventoryEntryRelationshipType type) {
        return new InventoryEntryRelationshipTypeView(
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
