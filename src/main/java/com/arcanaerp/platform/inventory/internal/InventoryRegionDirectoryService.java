package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryCountryDirectory;
import com.arcanaerp.platform.inventory.InventoryRegionDirectory;
import com.arcanaerp.platform.inventory.InventoryRegionView;
import com.arcanaerp.platform.inventory.RegisterInventoryRegionCommand;
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
class InventoryRegionDirectoryService implements InventoryRegionDirectory {

    private final InventoryRegionRepository inventoryRegionRepository;
    private final InventoryCountryDirectory inventoryCountryDirectory;
    private final Clock clock;

    @Override
    public InventoryRegionView registerRegion(RegisterInventoryRegionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String countryCode = normalizeRequired(command.countryCode(), "countryCode").toUpperCase();
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        if (!inventoryCountryDirectory.countryExists(countryCode)) {
            throw new IllegalArgumentException("Inventory country not found: " + countryCode);
        }
        if (inventoryRegionRepository.findByCountryCodeAndCode(countryCode, code).isPresent()) {
            throw new ConflictException(
                "Inventory region already exists for country: " + countryCode + " and code: " + code
            );
        }
        return toView(inventoryRegionRepository.save(
            InventoryRegion.create(countryCode, code, name, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryRegionView regionByCountryAndCode(String countryCode, String code) {
        String normalizedCountryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryRegionRepository.findByCountryCodeAndCode(normalizedCountryCode, normalizedCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory region not found for country: " + normalizedCountryCode + " and code: " + normalizedCode
            )));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean regionExists(String countryCode, String code) {
        String normalizedCountryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryRegionRepository.findByCountryCodeAndCode(normalizedCountryCode, normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryRegionView> listRegions(String countryCode, PageQuery pageQuery) {
        String normalizedCountryCode = normalizeOptionalUpper(countryCode);
        Sort sort = Sort.by(Sort.Direction.ASC, "countryCode").and(Sort.by("code"));
        Page<InventoryRegion> regions = normalizedCountryCode == null
            ? inventoryRegionRepository.findAll(pageQuery.toPageable(sort))
            : inventoryRegionRepository.findByCountryCode(normalizedCountryCode, pageQuery.toPageable(sort));
        return PageResult.from(regions).map(this::toView);
    }

    private InventoryRegionView toView(InventoryRegion region) {
        return new InventoryRegionView(
            region.getId(),
            region.getCountryCode(),
            region.getCode(),
            region.getName(),
            region.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("countryCode is required");
        }
        return value.trim().toUpperCase();
    }
}
