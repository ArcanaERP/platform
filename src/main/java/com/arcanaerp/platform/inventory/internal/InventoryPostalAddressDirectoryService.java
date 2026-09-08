package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryAddressPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryCountryDirectory;
import com.arcanaerp.platform.inventory.InventoryPostalAddressDirectory;
import com.arcanaerp.platform.inventory.InventoryPostalAddressMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryPostalAddressView;
import com.arcanaerp.platform.inventory.InventoryRegionDirectory;
import com.arcanaerp.platform.inventory.RegisterInventoryPostalAddressCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryPostalAddressMetadataCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryPostalAddressDirectoryService implements InventoryPostalAddressDirectory {

    private static final String OWNER_TYPE_LOCATION = "LOCATION";
    private static final String OWNER_TYPE_FACILITY = "FACILITY";

    private final InventoryPostalAddressRepository inventoryPostalAddressRepository;
    private final InventoryPostalAddressMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final InventoryAddressPurposeDirectory inventoryAddressPurposeDirectory;
    private final InventoryCountryDirectory inventoryCountryDirectory;
    private final InventoryRegionDirectory inventoryRegionDirectory;
    private final Clock clock;

    @Override
    public InventoryPostalAddressView registerPostalAddress(RegisterInventoryPostalAddressCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String ownerType = normalizeOwnerType(command.ownerType());
        String ownerCode = normalizeRequired(command.ownerCode(), "ownerCode").toUpperCase();
        String addressPurposeCode = normalizeRequired(command.addressPurposeCode(), "addressPurposeCode").toUpperCase();
        ensureOwnerExists(ownerType, ownerCode);
        ensureAddressPurposeExists(addressPurposeCode);
        ensureGeoReferenceExists(command.countryCode(), command.regionCode());
        if (
            inventoryPostalAddressRepository
                .findByOwnerTypeAndOwnerCodeAndAddressPurposeCode(ownerType, ownerCode, addressPurposeCode)
                .isPresent()
        ) {
            throw new ConflictException(
                "Inventory postal address already exists for owner: " + ownerType + "/" + ownerCode
                    + " and purpose: " + addressPurposeCode
            );
        }
        return toView(inventoryPostalAddressRepository.save(InventoryPostalAddress.create(
            ownerType,
            ownerCode,
            addressPurposeCode,
            command.addressLine1(),
            command.addressLine2(),
            command.city(),
            command.regionCode(),
            command.postalCode(),
            command.countryCode(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPostalAddressView postalAddressById(UUID id) {
        return toView(findPostalAddress(id));
    }

    @Override
    public InventoryPostalAddressView updatePostalAddressMetadata(
        UUID id,
        UpdateInventoryPostalAddressMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryPostalAddress address = findPostalAddress(id);
        String addressPurposeCode = normalizeRequired(command.addressPurposeCode(), "addressPurposeCode").toUpperCase();
        ensureAddressPurposeExists(addressPurposeCode);
        ensureGeoReferenceExists(command.countryCode(), command.regionCode());
        if (
            !address.getAddressPurposeCode().equals(addressPurposeCode)
                && inventoryPostalAddressRepository
                    .findByOwnerTypeAndOwnerCodeAndAddressPurposeCode(
                        address.getOwnerType(),
                        address.getOwnerCode(),
                        addressPurposeCode
                    )
                    .isPresent()
        ) {
            throw new ConflictException(
                "Inventory postal address already exists for owner: " + address.getOwnerType() + "/"
                    + address.getOwnerCode() + " and purpose: " + addressPurposeCode
            );
        }
        InventoryPostalAddressMetadataSnapshot previous = InventoryPostalAddressMetadataSnapshot.from(address);
        Instant changedAt = Instant.now(clock);
        address.updateMetadata(
            command.addressPurposeCode(),
            command.addressLine1(),
            command.addressLine2(),
            command.city(),
            command.regionCode(),
            command.postalCode(),
            command.countryCode(),
            changedAt
        );
        metadataChangeAuditRepository.save(InventoryPostalAddressMetadataChangeAudit.create(
            address.getId(),
            address.getOwnerType(),
            address.getOwnerCode(),
            previous,
            InventoryPostalAddressMetadataSnapshot.from(address),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryPostalAddressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryPostalAddressMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryPostalAddress address = findPostalAddress(id);
        Page<InventoryPostalAddressMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            address.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryPostalAddressView> listPostalAddresses(
        String ownerType,
        String ownerCode,
        String addressPurposeCode,
        PageQuery pageQuery
    ) {
        return PageResult.from(inventoryPostalAddressRepository.findFiltered(
            normalizeOptionalOwnerType(ownerType),
            normalizeOptionalUpper(ownerCode, "ownerCode"),
            normalizeOptionalUpper(addressPurposeCode, "addressPurposeCode"),
            pageQuery.toPageable(
                Sort.by(Sort.Direction.ASC, "ownerType")
                    .and(Sort.by("ownerCode"))
                    .and(Sort.by("addressPurposeCode"))
            )
        )).map(this::toView);
    }

    private InventoryPostalAddress findPostalAddress(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return inventoryPostalAddressRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory postal address not found for id: " + id));
    }

    private void ensureOwnerExists(String ownerType, String ownerCode) {
        if (OWNER_TYPE_LOCATION.equals(ownerType)) {
            if (inventoryLocationRepository.findByCode(ownerCode).isEmpty()) {
                throw new IllegalArgumentException("Inventory location not found: " + ownerCode);
            }
            return;
        }
        if (OWNER_TYPE_FACILITY.equals(ownerType)) {
            if (inventoryFacilityRepository.findByCode(ownerCode).isEmpty()) {
                throw new IllegalArgumentException("Inventory facility not found: " + ownerCode);
            }
            return;
        }
        throw new IllegalArgumentException("ownerType must be LOCATION or FACILITY");
    }

    private void ensureAddressPurposeExists(String addressPurposeCode) {
        if (!inventoryAddressPurposeDirectory.addressPurposeExists(addressPurposeCode)) {
            throw new IllegalArgumentException("Inventory address purpose not found: " + addressPurposeCode);
        }
    }

    private void ensureGeoReferenceExists(String countryCode, String regionCode) {
        String normalizedCountryCode = normalizeRequired(countryCode, "countryCode").toUpperCase();
        String normalizedRegionCode = normalizeOptionalUpper(regionCode, "regionCode");
        if (!inventoryCountryDirectory.countryExists(normalizedCountryCode)) {
            throw new IllegalArgumentException("Inventory country not found: " + normalizedCountryCode);
        }
        if (normalizedRegionCode != null && !inventoryRegionDirectory.regionExists(normalizedCountryCode, normalizedRegionCode)) {
            throw new IllegalArgumentException(
                "Inventory region not found for country: " + normalizedCountryCode + " and code: " + normalizedRegionCode
            );
        }
    }

    private InventoryPostalAddressView toView(InventoryPostalAddress address) {
        return new InventoryPostalAddressView(
            address.getId(),
            address.getOwnerType(),
            address.getOwnerCode(),
            address.getAddressPurposeCode(),
            address.getAddressLine1(),
            address.getAddressLine2(),
            address.getCity(),
            address.getRegionCode(),
            address.getPostalCode(),
            address.getCountryCode(),
            address.getCreatedAt(),
            address.getUpdatedAt()
        );
    }

    private InventoryPostalAddressMetadataChangeView toMetadataChangeView(
        InventoryPostalAddressMetadataChangeAudit audit
    ) {
        return new InventoryPostalAddressMetadataChangeView(
            audit.getId(),
            audit.getPostalAddressId(),
            audit.getOwnerType(),
            audit.getOwnerCode(),
            audit.getPreviousAddressPurposeCode(),
            audit.getCurrentAddressPurposeCode(),
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
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private static String normalizeOwnerType(String value) {
        String normalized = normalizeRequired(value, "ownerType").toUpperCase();
        if (!OWNER_TYPE_LOCATION.equals(normalized) && !OWNER_TYPE_FACILITY.equals(normalized)) {
            throw new IllegalArgumentException("ownerType must be LOCATION or FACILITY");
        }
        return normalized;
    }

    private static String normalizeOptionalOwnerType(String value) {
        return value == null ? null : normalizeOwnerType(value);
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
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

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }
}
