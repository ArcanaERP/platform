package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryCountryDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryFacilityMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFacilityView;
import com.arcanaerp.platform.inventory.InventoryLocationTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryRegionDirectory;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFacilityActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFacilityMetadataCommand;
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
class InventoryFacilityDirectoryService implements InventoryFacilityDirectory {

    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final InventoryFacilityActiveChangeAuditRepository activeChangeAuditRepository;
    private final InventoryFacilityMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final InventoryLocationTypeDirectory inventoryLocationTypeDirectory;
    private final InventoryCountryDirectory inventoryCountryDirectory;
    private final InventoryRegionDirectory inventoryRegionDirectory;
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
        ensureFacilityTypeExists(command.facilityTypeCode());
        ensureGeoReferenceExists(command.countryCode(), command.regionCode());
        return toView(inventoryFacilityRepository.save(InventoryFacility.create(
            code,
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
    public InventoryFacilityView updateFacilityActive(String code, UpdateInventoryFacilityActiveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        InventoryFacility facility = findFacility(normalizedCode);
        boolean previousActive = facility.isActive();
        Instant changedAt = Instant.now(clock);
        facility.setActive(command.active(), changedAt);
        activeChangeAuditRepository.save(InventoryFacilityActiveChangeAudit.create(
            facility.getId(),
            facility.getCode(),
            previousActive,
            facility.isActive(),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryFacilityRepository.save(facility));
    }

    @Override
    public InventoryFacilityView updateFacilityMetadata(String code, UpdateInventoryFacilityMetadataCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        String commandCode = normalizeRequired(command.code(), "code").toUpperCase();
        if (!normalizedCode.equals(commandCode)) {
            throw new IllegalArgumentException("code path variable must match command code");
        }
        ensureFacilityTypeExists(command.facilityTypeCode());
        ensureGeoReferenceExists(command.countryCode(), command.regionCode());
        InventoryFacility facility = findFacility(normalizedCode);
        InventoryFacilityMetadataSnapshot previous = InventoryFacilityMetadataSnapshot.from(facility);
        Instant changedAt = Instant.now(clock);
        facility.updateMetadata(
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
        metadataChangeAuditRepository.save(InventoryFacilityMetadataChangeAudit.create(
            facility.getId(),
            facility.getCode(),
            previous,
            InventoryFacilityMetadataSnapshot.from(facility),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryFacilityRepository.save(facility));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFacilityActiveChangeView> listActiveHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFacility facility = findFacility(code);
        Page<InventoryFacilityActiveChangeAudit> history = activeChangeAuditRepository.findHistoryFiltered(
            facility.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toActiveChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFacilityMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFacility facility = findFacility(code);
        Page<InventoryFacilityMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            facility.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
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

    private InventoryFacility findFacility(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryFacilityRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory facility not found for code: " + normalizedCode));
    }

    private InventoryFacilityView toView(InventoryFacility facility) {
        return new InventoryFacilityView(
            facility.getId(),
            facility.getCode(),
            facility.getName(),
            facility.getFacilityTypeCode(),
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

    private InventoryFacilityActiveChangeView toActiveChangeView(InventoryFacilityActiveChangeAudit audit) {
        return new InventoryFacilityActiveChangeView(
            audit.getId(),
            audit.getFacilityCode(),
            audit.isPreviousActive(),
            audit.isCurrentActive(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private InventoryFacilityMetadataChangeView toMetadataChangeView(InventoryFacilityMetadataChangeAudit audit) {
        return new InventoryFacilityMetadataChangeView(
            audit.getId(),
            audit.getFacilityCode(),
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

    private static String normalizeOptionalQuery(String query) {
        if (query == null) {
            return null;
        }
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        return query.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }

    private void ensureFacilityTypeExists(String facilityTypeCode) {
        String normalizedFacilityTypeCode = normalizeOptionalUpper(facilityTypeCode);
        if (normalizedFacilityTypeCode != null && !inventoryLocationTypeDirectory.locationTypeExists(normalizedFacilityTypeCode)) {
            throw new IllegalArgumentException("Inventory location type not found: " + normalizedFacilityTypeCode);
        }
    }

    private void ensureGeoReferenceExists(String countryCode, String regionCode) {
        String normalizedCountryCode = normalizeOptionalUpper(countryCode, "countryCode");
        String normalizedRegionCode = normalizeOptionalUpper(regionCode, "regionCode");
        if (normalizedCountryCode != null && !inventoryCountryDirectory.countryExists(normalizedCountryCode)) {
            throw new IllegalArgumentException("Inventory country not found: " + normalizedCountryCode);
        }
        if (normalizedRegionCode != null) {
            if (normalizedCountryCode == null) {
                throw new IllegalArgumentException("countryCode is required when regionCode is supplied");
            }
            if (!inventoryRegionDirectory.regionExists(normalizedCountryCode, normalizedRegionCode)) {
                throw new IllegalArgumentException(
                    "Inventory region not found for country: " + normalizedCountryCode + " and code: " + normalizedRegionCode
                );
            }
        }
    }

    private static String normalizeOptionalUpper(String value) {
        return normalizeOptionalUpper(value, "facilityTypeCode");
    }

    private static String normalizeOptionalUpper(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }
}
