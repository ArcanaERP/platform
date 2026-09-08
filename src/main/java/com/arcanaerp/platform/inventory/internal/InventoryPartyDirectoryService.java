package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPartyDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyView;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyCommand;
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
class InventoryPartyDirectoryService implements InventoryPartyDirectory {

    private final InventoryPartyRepository inventoryPartyRepository;
    private final Clock clock;

    @Override
    public InventoryPartyView registerParty(RegisterInventoryPartyCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        String description = normalizeRequired(command.description(), "description");
        if (inventoryPartyRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Inventory party already exists for code: " + code);
        }
        return toView(inventoryPartyRepository.save(
            InventoryParty.create(code, description, Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPartyView partyByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toView(inventoryPartyRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory party not found for code: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean partyExists(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return inventoryPartyRepository.findByCode(normalizedCode).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryPartyView> listParties(PageQuery pageQuery) {
        return PageResult.from(
            inventoryPartyRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code")))
        ).map(this::toView);
    }

    private InventoryPartyView toView(InventoryParty party) {
        return new InventoryPartyView(
            party.getId(),
            party.getCode(),
            party.getDescription(),
            party.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
