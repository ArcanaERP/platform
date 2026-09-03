package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationCommand;
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
class InventoryLocationDirectoryService implements InventoryLocationDirectory {

    private final InventoryLocationRepository inventoryLocationRepository;
    private final Clock clock;

    @Override
    public InventoryLocationView registerLocation(RegisterInventoryLocationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        if (inventoryLocationRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory location already exists for code: " + code);
        }

        return toView(inventoryLocationRepository.save(InventoryLocation.create(code, name, Instant.now(clock))));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLocationView locationByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryLocationView> listLocations(Boolean active, PageQuery pageQuery) {
        Sort sort = Sort.by(Sort.Direction.ASC, "code");
        Page<InventoryLocation> locations = active == null
            ? inventoryLocationRepository.findAll(pageQuery.toPageable(sort))
            : inventoryLocationRepository.findByActive(active, pageQuery.toPageable(sort));
        return PageResult.from(locations).map(this::toView);
    }

    private InventoryLocationView toView(InventoryLocation location) {
        return new InventoryLocationView(
            location.getId(),
            location.getCode(),
            location.getName(),
            location.isActive(),
            location.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
