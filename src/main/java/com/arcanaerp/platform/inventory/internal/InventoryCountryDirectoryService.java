package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryCountryDirectory;
import com.arcanaerp.platform.inventory.InventoryCountryView;
import com.arcanaerp.platform.inventory.RegisterInventoryCountryCommand;
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
class InventoryCountryDirectoryService implements InventoryCountryDirectory {

    private final InventoryCountryRepository inventoryCountryRepository;
    private final Clock clock;

    @Override
    public InventoryCountryView registerCountry(RegisterInventoryCountryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String name = normalizeRequired(command.name(), "name");
        if (inventoryCountryRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory country already exists for code: " + code);
        }
        return toView(inventoryCountryRepository.save(InventoryCountry.create(code, name, Instant.now(clock))));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCountryView countryByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryCountryRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory country not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean countryExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryCountryRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryCountryView> listCountries(PageQuery pageQuery) {
        return PageResult.from(
            inventoryCountryRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryCountryView toView(InventoryCountry country) {
        return new InventoryCountryView(
            country.getId(),
            country.getCode(),
            country.getName(),
            country.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
