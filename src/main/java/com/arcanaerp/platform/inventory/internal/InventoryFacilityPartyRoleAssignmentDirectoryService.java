package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFacilityPartyRoleAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityPartyRoleAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityPartyRoleAssignmentCommand;
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
class InventoryFacilityPartyRoleAssignmentDirectoryService
    implements InventoryFacilityPartyRoleAssignmentDirectory {

    private final InventoryFacilityPartyRoleAssignmentRepository assignmentRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final Clock clock;

    @Override
    public InventoryFacilityPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFacilityPartyRoleAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryFacility facility = findFacility(command.facilityCode());
        if (!facility.isActive()) {
            throw new IllegalArgumentException("Inventory facility is inactive: " + facility.getCode());
        }
        String partyCode = normalizeRequired(command.partyCode(), "partyCode").toUpperCase();
        String roleTypeCode = normalizeRequired(command.roleTypeCode(), "roleTypeCode").toUpperCase();
        if (
            assignmentRepository.findByInventoryFacilityIdAndPartyCodeAndRoleTypeCode(
                facility.getId(),
                partyCode,
                roleTypeCode
            ).isPresent()
        ) {
            throw new ConflictException(
                "Inventory facility party role assignment already exists for facility: "
                    + facility.getCode()
                    + ", party: "
                    + partyCode
                    + ", role type: "
                    + roleTypeCode
            );
        }
        return toView(assignmentRepository.save(InventoryFacilityPartyRoleAssignment.create(
            facility,
            partyCode,
            roleTypeCode,
            command.comments(),
            command.fromDate(),
            command.thruDate(),
            command.assignedBy(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFacilityPartyRoleAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFacilityPartyRoleAssignmentView> listAssignments(
        String facilityCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        PageQuery pageQuery
    ) {
        Page<InventoryFacilityPartyRoleAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(facilityCode, "facilityCode"),
            normalizeOptionalUpper(partyCode, "partyCode"),
            normalizeOptionalUpper(roleTypeCode, "roleTypeCode"),
            normalizeOptionalLower(assignedBy, "assignedBy"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "facilityCode").and(Sort.by("partyCode")))
        );
        return PageResult.from(assignments).map(this::toView);
    }

    private InventoryFacility findFacility(String facilityCode) {
        String normalizedFacilityCode = normalizeRequired(facilityCode, "facilityCode").toUpperCase();
        return inventoryFacilityRepository.findByCode(normalizedFacilityCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory facility not found for code: " + normalizedFacilityCode
            ));
    }

    private InventoryFacilityPartyRoleAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory facility party role assignment not found for id: " + id
            ));
    }

    private InventoryFacilityPartyRoleAssignmentView toView(InventoryFacilityPartyRoleAssignment assignment) {
        return new InventoryFacilityPartyRoleAssignmentView(
            assignment.getId(),
            assignment.getInventoryFacilityId(),
            assignment.getFacilityCode(),
            assignment.getPartyCode(),
            assignment.getRoleTypeCode(),
            assignment.getComments(),
            assignment.getFromDate(),
            assignment.getThruDate(),
            assignment.getAssignedBy(),
            assignment.getAssignedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toUpperCase();
    }

    private static String normalizeOptionalLower(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toLowerCase();
    }
}
