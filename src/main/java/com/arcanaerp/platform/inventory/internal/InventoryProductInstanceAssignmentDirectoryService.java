package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentReleaseView;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentView;
import com.arcanaerp.platform.inventory.ReleaseInventoryProductInstanceAssignmentCommand;
import com.arcanaerp.platform.inventory.RegisterInventoryProductInstanceAssignmentCommand;
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
class InventoryProductInstanceAssignmentDirectoryService implements InventoryProductInstanceAssignmentDirectory {

    private final InventoryProductInstanceAssignmentRepository assignmentRepository;
    private final InventoryProductInstanceAssignmentReleaseAuditRepository releaseAuditRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final Clock clock;

    @Override
    public InventoryProductInstanceAssignmentView registerAssignment(
        RegisterInventoryProductInstanceAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryItem item = findItem(command.sku(), command.locationCode());
        String productInstanceCode = normalizeRequired(command.productInstanceCode(), "productInstanceCode").toUpperCase();
        if (assignmentRepository.findByInventoryItemIdAndProductInstanceCode(item.getId(), productInstanceCode).isPresent()) {
            throw new ConflictException(
                "Inventory product instance assignment already exists for SKU: "
                    + item.getSku()
                    + " at location: "
                    + item.getLocationCode()
                    + " and product instance: "
                    + productInstanceCode
            );
        }
        return toView(assignmentRepository.save(InventoryProductInstanceAssignment.create(
            item,
            productInstanceCode,
            command.assignedBy(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryProductInstanceAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    public InventoryProductInstanceAssignmentView releaseAssignment(
        UUID id,
        ReleaseInventoryProductInstanceAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (id == null || command.id() == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (!id.equals(command.id())) {
            throw new IllegalArgumentException("id path variable must match command id");
        }
        InventoryProductInstanceAssignment assignment = findAssignment(id);
        Instant releasedAt = Instant.now(clock);
        assignment.release(command.reason(), command.releasedBy(), releasedAt);
        releaseAuditRepository.save(InventoryProductInstanceAssignmentReleaseAudit.create(
            assignment,
            command.reason(),
            command.releasedBy(),
            releasedAt
        ));
        return toView(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryProductInstanceAssignmentReleaseView> listReleaseHistory(
        UUID id,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    ) {
        InventoryProductInstanceAssignment assignment = findAssignment(id);
        Page<InventoryProductInstanceAssignmentReleaseAudit> history = releaseAuditRepository.findHistoryFiltered(
            assignment.getId(),
            normalizeOptionalLower(releasedBy, "releasedBy"),
            releasedAtFrom,
            releasedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "releasedAt"))
        );
        return PageResult.from(history).map(this::toReleaseView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryProductInstanceAssignmentView> listAssignments(
        String sku,
        String locationCode,
        String productInstanceCode,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    ) {
        Page<InventoryProductInstanceAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(sku, "sku"),
            normalizeOptionalUpper(locationCode, "locationCode"),
            normalizeOptionalUpper(productInstanceCode, "productInstanceCode"),
            normalizeOptionalLower(assignedBy, "assignedBy"),
            active,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "productInstanceCode").and(Sort.by("sku")))
        );
        return PageResult.from(assignments).map(this::toView);
    }

    private InventoryItem findItem(String sku, String locationCode) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedLocationCode = normalizeRequired(locationCode, "locationCode").toUpperCase();
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: " + normalizedSku + " at location: " + normalizedLocationCode
            ));
    }

    private InventoryProductInstanceAssignmentView toView(InventoryProductInstanceAssignment assignment) {
        return new InventoryProductInstanceAssignmentView(
            assignment.getId(),
            assignment.getInventoryItemId(),
            assignment.getSku(),
            assignment.getLocationCode(),
            assignment.getProductInstanceCode(),
            assignment.getAssignedBy(),
            assignment.getAssignedAt(),
            assignment.isActive(),
            assignment.getReleaseReason(),
            assignment.getReleasedBy(),
            assignment.getReleasedAt()
        );
    }

    private InventoryProductInstanceAssignmentReleaseView toReleaseView(
        InventoryProductInstanceAssignmentReleaseAudit audit
    ) {
        return new InventoryProductInstanceAssignmentReleaseView(
            audit.getId(),
            audit.getAssignmentId(),
            audit.getInventoryItemId(),
            audit.getSku(),
            audit.getLocationCode(),
            audit.getProductInstanceCode(),
            audit.getReason(),
            audit.getReleasedBy(),
            audit.getReleasedAt()
        );
    }

    private InventoryProductInstanceAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory product instance assignment not found for id: " + id
            ));
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
