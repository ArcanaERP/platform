package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFacilityDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityView;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityCommand;
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
class InventoryFacilityDirectoryService implements InventoryFacilityDirectory {

    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final Clock clock;

    @Override
    public InventoryFacilityView registerFacility(RegisterInventoryFacilityCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        if (inventoryFacilityRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory facility already exists for code: " + code);
        }
        return toView(inventoryFacilityRepository.save(InventoryFacility.create(
            code,
            command.name(),
            command.addressLine1(),
            command.addressLine2(),
            command.city(),
            command.regionCode(),
            command.postalCode(),
            command.countryCode(),
            command.contactName(),
            command.contactEmail(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFacilityView facilityByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryFacilityRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory facility not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFacilityView> listFacilities(Boolean active, String query, PageQuery pageQuery) {
        return PageResult.from(inventoryFacilityRepository.findFiltered(
            active,
            normalizeOptionalQuery(query),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code"))
        )).map(this::toView);
    }

    private InventoryFacilityView toView(InventoryFacility facility) {
        return new InventoryFacilityView(
            facility.getId(),
            facility.getCode(),
            facility.getName(),
            facility.getAddressLine1(),
            facility.getAddressLine2(),
            facility.getCity(),
            facility.getRegionCode(),
            facility.getPostalCode(),
            facility.getCountryCode(),
            facility.getContactName(),
            facility.getContactEmail(),
            facility.isActive(),
            facility.getCreatedAt(),
            facility.getUpdatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalQuery(String query) {
        if (query == null) {
            return null;
        }
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        return query.trim().toUpperCase();
    }
}
