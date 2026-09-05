package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryLocationView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationMetadataCommand;
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
    private final InventoryLocationMetadataChangeAuditRepository metadataChangeAuditRepository;
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

        return toView(inventoryLocationRepository.save(InventoryLocation.create(
            code,
            name,
            command.facilityTypeCode(),
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
    public InventoryLocationView locationByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode)));
    }

    @Override
    public InventoryLocationView updateLocationActive(String code, UpdateInventoryLocationActiveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryLocation location = inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode));
        location.setActive(command.active(), Instant.now(clock));
        return toView(inventoryLocationRepository.save(location));
    }

    @Override
    public InventoryLocationView updateLocationMetadata(String code, UpdateInventoryLocationMetadataCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryLocation location = inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode));
        InventoryLocationMetadataSnapshot previous = InventoryLocationMetadataSnapshot.from(location);
        Instant changedAt = Instant.now(clock);
        location.updateMetadata(
            command.name(),
            command.facilityTypeCode(),
            command.addressLine1(),
            command.addressLine2(),
            command.city(),
            command.regionCode(),
            command.postalCode(),
            command.countryCode(),
            command.contactName(),
            command.contactEmail(),
            changedAt
        );
        metadataChangeAuditRepository.save(InventoryLocationMetadataChangeAudit.create(
            location.getId(),
            location.getCode(),
            previous,
            InventoryLocationMetadataSnapshot.from(location),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryLocationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryLocationMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryLocation location = findLocation(code);
        Page<InventoryLocationMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            location.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
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

    private InventoryLocation findLocation(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode));
    }

    private InventoryLocationView toView(InventoryLocation location) {
        return new InventoryLocationView(
            location.getId(),
            location.getCode(),
            location.getName(),
            location.getFacilityTypeCode(),
            location.getAddressLine1(),
            location.getAddressLine2(),
            location.getCity(),
            location.getRegionCode(),
            location.getPostalCode(),
            location.getCountryCode(),
            location.getContactName(),
            location.getContactEmail(),
            location.isActive(),
            location.getCreatedAt(),
            location.getUpdatedAt()
        );
    }

    private InventoryLocationMetadataChangeView toMetadataChangeView(InventoryLocationMetadataChangeAudit audit) {
        return new InventoryLocationMetadataChangeView(
            audit.getId(),
            audit.getLocationCode(),
            audit.getPreviousName(),
            audit.getCurrentName(),
            audit.getPreviousFacilityTypeCode(),
            audit.getCurrentFacilityTypeCode(),
            audit.getPreviousAddressLine1(),
            audit.getCurrentAddressLine1(),
            audit.getPreviousAddressLine2(),
            audit.getCurrentAddressLine2(),
            audit.getPreviousCity(),
            audit.getCurrentCity(),
            audit.getPreviousRegionCode(),
            audit.getCurrentRegionCode(),
            audit.getPreviousPostalCode(),
            audit.getCurrentPostalCode(),
            audit.getPreviousCountryCode(),
            audit.getCurrentCountryCode(),
            audit.getPreviousContactName(),
            audit.getCurrentContactName(),
            audit.getPreviousContactEmail(),
            audit.getCurrentContactEmail(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
