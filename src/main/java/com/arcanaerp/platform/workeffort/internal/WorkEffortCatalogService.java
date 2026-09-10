package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.workeffort.AssociatedWorkEffortView;
import com.arcanaerp.platform.workeffort.AssignWorkEffortCommand;
import com.arcanaerp.platform.workeffort.ChangeWorkEffortStatusCommand;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.DailyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.OrderRequirementCommitmentView;
import com.arcanaerp.platform.workeffort.RegisterAssociatedWorkEffortCommand;
import com.arcanaerp.platform.workeffort.RegisterOrderRequirementCommitmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortAssociationCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortAssociationTypeCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortFixedAssetAssignmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortInventoryAssignmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortPartyAssignmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkEffortRoleTypeAssignmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkRequirementFulfillmentCommand;
import com.arcanaerp.platform.workeffort.RegisterWorkOrderItemFulfillmentCommand;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssociationTypeView;
import com.arcanaerp.platform.workeffort.WorkEffortAssociationView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentSummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortFixedAssetAssignmentView;
import com.arcanaerp.platform.workeffort.WorkEffortInventoryAssignmentView;
import com.arcanaerp.platform.workeffort.WorkEffortPartyAssignmentView;
import com.arcanaerp.platform.workeffort.WorkEffortRoleTypeAssignmentView;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import com.arcanaerp.platform.workeffort.WorkEffortStatusChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortView;
import com.arcanaerp.platform.workeffort.WorkRequirementFulfillmentView;
import com.arcanaerp.platform.workeffort.WorkOrderItemFulfillmentView;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class WorkEffortCatalogService implements WorkEffortCatalog {

    private final WorkEffortRepository workEffortRepository;
    private final AssociatedWorkEffortRepository associatedWorkEffortRepository;
    private final WorkOrderItemFulfillmentRepository workOrderItemFulfillmentRepository;
    private final OrderRequirementCommitmentRepository orderRequirementCommitmentRepository;
    private final WorkRequirementFulfillmentRepository workRequirementFulfillmentRepository;
    private final WorkEffortAssociationTypeRepository workEffortAssociationTypeRepository;
    private final WorkEffortAssociationRepository workEffortAssociationRepository;
    private final WorkEffortStatusChangeAuditRepository workEffortStatusChangeAuditRepository;
    private final WorkEffortAssignmentChangeAuditRepository workEffortAssignmentChangeAuditRepository;
    private final WorkEffortFixedAssetAssignmentRepository workEffortFixedAssetAssignmentRepository;
    private final WorkEffortInventoryAssignmentRepository workEffortInventoryAssignmentRepository;
    private final WorkEffortPartyAssignmentRepository workEffortPartyAssignmentRepository;
    private final WorkEffortRoleTypeAssignmentRepository workEffortRoleTypeAssignmentRepository;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public WorkEffortView createWorkEffort(CreateWorkEffortCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        String assignedTo = normalizeAssignedTo(command.assignedTo());
        Instant now = Instant.now(clock);

        if (workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber).isPresent()) {
            throw new ConflictException("Work effort already exists for tenant/effortNumber: " + tenantCode + "/" + effortNumber);
        }
        if (!identityActorLookup.actorExists(tenantCode, assignedTo)) {
            throw new IllegalArgumentException("work effort assignee not found in tenant: " + tenantCode + "/" + assignedTo);
        }

        WorkEffort created = workEffortRepository.save(
            WorkEffort.create(
                tenantCode,
                effortNumber,
                command.name(),
                command.description(),
                command.status(),
                assignedTo,
                command.dueAt(),
                now
            )
        );
        return toView(created);
    }

    @Override
    public AssociatedWorkEffortView registerAssociatedWorkEffort(RegisterAssociatedWorkEffortCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toAssociatedWorkEffortView(associatedWorkEffortRepository.save(
            AssociatedWorkEffort.create(
                workEffort,
                command.associatedRecordId(),
                command.associatedRecordType()
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AssociatedWorkEffortView associatedWorkEffortById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toAssociatedWorkEffortView(associatedWorkEffortRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Associated work effort not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssociatedWorkEffortView> listAssociatedWorkEfforts(
        String tenantCode,
        String effortNumber,
        Long associatedRecordId,
        String associatedRecordType,
        PageQuery pageQuery
    ) {
        Page<AssociatedWorkEffort> page = associatedWorkEffortRepository.findAssociatedWorkEffortsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            associatedRecordId,
            normalizeOptional(associatedRecordType, "associatedRecordType"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "associatedRecordType"))
        );
        return PageResult.from(page).map(this::toAssociatedWorkEffortView);
    }

    @Override
    public WorkOrderItemFulfillmentView registerWorkOrderItemFulfillment(
        RegisterWorkOrderItemFulfillmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toWorkOrderItemFulfillmentView(workOrderItemFulfillmentRepository.save(
            WorkOrderItemFulfillment.create(
                workEffort,
                command.orderLineItemId(),
                command.description(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderItemFulfillmentView workOrderItemFulfillmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toWorkOrderItemFulfillmentView(workOrderItemFulfillmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work order item fulfillment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkOrderItemFulfillmentView> listWorkOrderItemFulfillments(
        String tenantCode,
        String effortNumber,
        Long orderLineItemId,
        PageQuery pageQuery
    ) {
        Page<WorkOrderItemFulfillment> page = workOrderItemFulfillmentRepository.findFulfillmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            orderLineItemId,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toWorkOrderItemFulfillmentView);
    }

    @Override
    public OrderRequirementCommitmentView registerOrderRequirementCommitment(
        RegisterOrderRequirementCommitmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        return toOrderRequirementCommitmentView(orderRequirementCommitmentRepository.save(
            OrderRequirementCommitment.create(
                command.orderLineItemId(),
                command.requirementId(),
                command.description(),
                command.quantity(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderRequirementCommitmentView orderRequirementCommitmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toOrderRequirementCommitmentView(orderRequirementCommitmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Order requirement commitment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderRequirementCommitmentView> listOrderRequirementCommitments(
        Long orderLineItemId,
        Long requirementId,
        PageQuery pageQuery
    ) {
        Page<OrderRequirementCommitment> page = orderRequirementCommitmentRepository.findCommitmentsFiltered(
            orderLineItemId,
            requirementId,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toOrderRequirementCommitmentView);
    }

    @Override
    public WorkRequirementFulfillmentView registerWorkRequirementFulfillment(
        RegisterWorkRequirementFulfillmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toWorkRequirementFulfillmentView(workRequirementFulfillmentRepository.save(
            WorkRequirementFulfillment.create(
                workEffort,
                command.requirementId(),
                command.description(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkRequirementFulfillmentView workRequirementFulfillmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toWorkRequirementFulfillmentView(workRequirementFulfillmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work requirement fulfillment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkRequirementFulfillmentView> listWorkRequirementFulfillments(
        String tenantCode,
        String effortNumber,
        Long requirementId,
        PageQuery pageQuery
    ) {
        Page<WorkRequirementFulfillment> page = workRequirementFulfillmentRepository.findFulfillmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            requirementId,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toWorkRequirementFulfillmentView);
    }

    @Override
    public WorkEffortAssociationTypeView registerAssociationType(RegisterWorkEffortAssociationTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String code = normalizeRequired(command.code(), "code").toUpperCase();
        if (workEffortAssociationTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Work effort association type already exists: " + code);
        }
        return toAssociationTypeView(workEffortAssociationTypeRepository.save(
            WorkEffortAssociationType.create(
                code,
                command.name(),
                command.description(),
                command.parentTypeCode(),
                command.validFromRoleTypeCode(),
                command.validToRoleTypeCode(),
                command.externalIdentifier(),
                command.externalIdSource(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortAssociationTypeView associationTypeByCode(String code) {
        String normalizedCode = normalizeRequired(code, "code").toUpperCase();
        return toAssociationTypeView(workEffortAssociationTypeRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Work effort association type not found: " + normalizedCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssociationTypeView> listAssociationTypes(String parentTypeCode, PageQuery pageQuery) {
        Page<WorkEffortAssociationType> page = workEffortAssociationTypeRepository.findTypesFiltered(
            normalizeOptionalUpper(parentTypeCode, "parentTypeCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "code"))
        );
        return PageResult.from(page).map(this::toAssociationTypeView);
    }

    @Override
    public WorkEffortAssociationView registerAssociation(RegisterWorkEffortAssociationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String associationTypeCode = normalizeRequired(command.associationTypeCode(), "associationTypeCode").toUpperCase();
        WorkEffortAssociationType associationType = workEffortAssociationTypeRepository.findByCode(associationTypeCode)
            .orElseThrow(() -> new NoSuchElementException("Work effort association type not found: " + associationTypeCode));
        WorkEffort fromWorkEffort = findWorkEffort(tenantCode, command.fromEffortNumber());
        WorkEffort toWorkEffort = findWorkEffort(tenantCode, command.toEffortNumber());
        return toAssociationView(workEffortAssociationRepository.save(
            WorkEffortAssociation.create(
                associationType,
                command.description(),
                fromWorkEffort,
                toWorkEffort,
                command.fromRoleTypeCode(),
                command.toRoleTypeCode(),
                command.relationshipTypeCode(),
                command.effectiveFrom(),
                command.effectiveThru(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortAssociationView associationById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toAssociationView(workEffortAssociationRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work effort association not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssociationView> listAssociations(
        String tenantCode,
        String associationTypeCode,
        String fromEffortNumber,
        String toEffortNumber,
        String relationshipTypeCode,
        Instant effectiveFrom,
        Instant effectiveThru,
        PageQuery pageQuery
    ) {
        Page<WorkEffortAssociation> page = workEffortAssociationRepository.findAssociationsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(associationTypeCode, "associationTypeCode"),
            normalizeOptionalUpper(fromEffortNumber, "fromEffortNumber"),
            normalizeOptionalUpper(toEffortNumber, "toEffortNumber"),
            normalizeOptionalUpper(relationshipTypeCode, "relationshipTypeCode"),
            effectiveFrom,
            effectiveThru,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toAssociationView);
    }

    @Override
    public WorkEffortFixedAssetAssignmentView registerFixedAssetAssignment(
        RegisterWorkEffortFixedAssetAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toFixedAssetAssignmentView(workEffortFixedAssetAssignmentRepository.save(
            WorkEffortFixedAssetAssignment.create(workEffort, command.fixedAssetCode(), Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortFixedAssetAssignmentView fixedAssetAssignmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toFixedAssetAssignmentView(workEffortFixedAssetAssignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work effort fixed asset assignment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortFixedAssetAssignmentView> listFixedAssetAssignments(
        String tenantCode,
        String effortNumber,
        String fixedAssetCode,
        PageQuery pageQuery
    ) {
        Page<WorkEffortFixedAssetAssignment> page = workEffortFixedAssetAssignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            normalizeOptionalUpper(fixedAssetCode, "fixedAssetCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toFixedAssetAssignmentView);
    }

    @Override
    public WorkEffortInventoryAssignmentView registerInventoryAssignment(
        RegisterWorkEffortInventoryAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toInventoryAssignmentView(workEffortInventoryAssignmentRepository.save(
            WorkEffortInventoryAssignment.create(workEffort, command.inventoryEntryCode(), Instant.now(clock))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortInventoryAssignmentView inventoryAssignmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toInventoryAssignmentView(workEffortInventoryAssignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work effort inventory assignment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortInventoryAssignmentView> listInventoryAssignments(
        String tenantCode,
        String effortNumber,
        String inventoryEntryCode,
        PageQuery pageQuery
    ) {
        Page<WorkEffortInventoryAssignment> page = workEffortInventoryAssignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            normalizeOptionalUpper(inventoryEntryCode, "inventoryEntryCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toInventoryAssignmentView);
    }

    @Override
    public WorkEffortPartyAssignmentView registerPartyAssignment(
        RegisterWorkEffortPartyAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toPartyAssignmentView(workEffortPartyAssignmentRepository.save(
            WorkEffortPartyAssignment.create(
                workEffort,
                command.partyCode(),
                command.roleTypeCode(),
                command.assignedFrom(),
                command.assignedThru(),
                command.comments(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortPartyAssignmentView partyAssignmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toPartyAssignmentView(workEffortPartyAssignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work effort party assignment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortPartyAssignmentView> listPartyAssignments(
        String tenantCode,
        String effortNumber,
        String partyCode,
        String roleTypeCode,
        Instant assignedFrom,
        Instant assignedThru,
        PageQuery pageQuery
    ) {
        Page<WorkEffortPartyAssignment> page = workEffortPartyAssignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            normalizeOptionalUpper(partyCode, "partyCode"),
            normalizeOptionalUpper(roleTypeCode, "roleTypeCode"),
            assignedFrom,
            assignedThru,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toPartyAssignmentView);
    }

    @Override
    public WorkEffortRoleTypeAssignmentView registerRoleTypeAssignment(
        RegisterWorkEffortRoleTypeAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        WorkEffort workEffort = findWorkEffort(command.tenantCode(), command.effortNumber());
        return toRoleTypeAssignmentView(workEffortRoleTypeAssignmentRepository.save(
            WorkEffortRoleTypeAssignment.create(workEffort, command.roleTypeCode())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortRoleTypeAssignmentView roleTypeAssignmentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toRoleTypeAssignmentView(workEffortRoleTypeAssignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Work effort role type assignment not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortRoleTypeAssignmentView> listRoleTypeAssignments(
        String tenantCode,
        String effortNumber,
        String roleTypeCode,
        PageQuery pageQuery
    ) {
        Page<WorkEffortRoleTypeAssignment> page = workEffortRoleTypeAssignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(tenantCode, "tenantCode"),
            normalizeOptionalUpper(effortNumber, "effortNumber"),
            normalizeOptionalUpper(roleTypeCode, "roleTypeCode"),
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "roleTypeCode"))
        );
        return PageResult.from(page).map(this::toRoleTypeAssignmentView);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortView getWorkEffort(String tenantCode, String effortNumber) {
        return toView(findWorkEffort(tenantCode, effortNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortAssignmentSummaryView getWorkEffortAssignment(String tenantCode, String effortNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        return toAssignmentSummaryView(workEffort);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortView> listWorkEfforts(
        String tenantCode,
        PageQuery pageQuery,
        WorkEffortStatus status,
        String assignedTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        Page<WorkEffort> page = findWorkEfforts(normalizedTenantCode, status, normalizedAssignedTo, pageQuery);
        return PageResult.from(page).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssignmentActivitySummaryView> listAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        Page<WorkEffortAssignmentChangeAuditRepository.AssignmentActivitySummaryProjection> page =
            workEffortAssignmentChangeAuditRepository.summarizeAssignmentActivity(
                normalizedTenantCode,
                normalizedAssignedTo,
                assignedAtFrom,
                assignedAtTo,
                pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "currentAssignedTo"))
            );
        return PageResult.from(page).map(summary -> new WorkEffortAssignmentActivitySummaryView(
            normalizedTenantCode,
            summary.getAssignedTo(),
            summary.getAssignmentCount(),
            summary.getFirstAssignedAt(),
            summary.getLastAssignedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortAssignmentActivitySummaryView> listDailyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortAssignmentActivitySummaryView> listWeeklyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortAssignmentActivityByAssigneeSummaryView> listDailyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView> listWeeklyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortAssignmentActivitySummaryView> listMonthlyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getAssignedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView> listMonthlyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getAssignedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    public WorkEffortView changeWorkEffortStatus(ChangeWorkEffortStatusCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        WorkEffortStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeAssignedTo(command.changedBy());

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + tenantCode + "/" + effortNumber
            ));
        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException("work effort status actor not found in tenant: " + tenantCode + "/" + changedBy);
        }

        WorkEffortStatus previousStatus = workEffort.getStatus();
        workEffort.transitionTo(targetStatus);
        WorkEffort saved = workEffortRepository.save(workEffort);
        if (previousStatus != saved.getStatus()) {
            workEffortStatusChangeAuditRepository.save(
                WorkEffortStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    tenantCode,
                    reason,
                    changedBy,
                    Instant.now(clock)
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortStatusChangeView> listStatusHistory(
        String tenantCode,
        String effortNumber,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeAssignedTo(changedBy);

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        Page<WorkEffortStatusChangeAudit> page = workEffortStatusChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            normalizedTenantCode,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(page).map(audit -> new WorkEffortStatusChangeView(
            audit.getId(),
            workEffort.getEffortNumber(),
            audit.getPreviousStatus(),
            audit.getCurrentStatus(),
            audit.getTenantCode(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ));
    }

    @Override
    public WorkEffortView assignWorkEffort(AssignWorkEffortCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        String assignedTo = normalizeAssignedTo(command.assignedTo());
        String assignedBy = normalizeActorEmail(command.assignedBy(), "assignedBy");
        String reason = normalizeRequired(command.reason(), "reason");

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + tenantCode + "/" + effortNumber
            ));
        if (!identityActorLookup.actorExists(tenantCode, assignedTo)) {
            throw new IllegalArgumentException("work effort assignee not found in tenant: " + tenantCode + "/" + assignedTo);
        }
        if (!identityActorLookup.actorExists(tenantCode, assignedBy)) {
            throw new IllegalArgumentException("work effort assignment actor not found in tenant: " + tenantCode + "/" + assignedBy);
        }

        String previousAssignedTo = workEffort.getAssignedTo();
        workEffort.assignTo(assignedTo);
        WorkEffort saved = workEffortRepository.save(workEffort);
        if (!previousAssignedTo.equals(saved.getAssignedTo())) {
            workEffortAssignmentChangeAuditRepository.save(
                WorkEffortAssignmentChangeAudit.create(
                    saved.getId(),
                    previousAssignedTo,
                    saved.getAssignedTo(),
                    tenantCode,
                    reason,
                    assignedBy,
                    Instant.now(clock)
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssignmentChangeView> listAssignmentHistory(
        String tenantCode,
        String effortNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        String normalizedAssignedBy = assignedBy == null ? null : normalizeActorEmail(assignedBy, "assignedBy");

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        Page<WorkEffortAssignmentChangeAudit> page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            normalizedTenantCode,
            normalizedAssignedTo,
            normalizedAssignedBy,
            assignedAtFrom,
            assignedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "assignedAt"))
        );
        return PageResult.from(page).map(audit -> new WorkEffortAssignmentChangeView(
            audit.getId(),
            workEffort.getEffortNumber(),
            audit.getPreviousAssignedTo(),
            audit.getCurrentAssignedTo(),
            audit.getTenantCode(),
            audit.getReason(),
            audit.getAssignedBy(),
            audit.getAssignedAt()
        ));
    }

    private Page<WorkEffort> findWorkEfforts(
        String tenantCode,
        WorkEffortStatus status,
        String assignedTo,
        PageQuery pageQuery
    ) {
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && assignedTo != null) {
            return workEffortRepository.findByTenantCodeAndStatusAndAssignedTo(tenantCode, status, assignedTo, pageable);
        }
        if (status != null) {
            return workEffortRepository.findByTenantCodeAndStatus(tenantCode, status, pageable);
        }
        if (assignedTo != null) {
            return workEffortRepository.findByTenantCodeAndAssignedTo(tenantCode, assignedTo, pageable);
        }
        return workEffortRepository.findByTenantCode(tenantCode, pageable);
    }

    private WorkEffortView toView(WorkEffort workEffort) {
        return new WorkEffortView(
            workEffort.getId(),
            workEffort.getTenantCode(),
            workEffort.getEffortNumber(),
            workEffort.getName(),
            workEffort.getDescription(),
            workEffort.getStatus(),
            workEffort.getAssignedTo(),
            workEffort.getDueAt(),
            workEffort.getCreatedAt()
        );
    }

    private WorkEffortAssignmentSummaryView toAssignmentSummaryView(WorkEffort workEffort) {
        return new WorkEffortAssignmentSummaryView(
            workEffort.getId(),
            workEffort.getTenantCode(),
            workEffort.getEffortNumber(),
            workEffort.getAssignedTo()
        );
    }

    private AssociatedWorkEffortView toAssociatedWorkEffortView(AssociatedWorkEffort associatedWorkEffort) {
        return new AssociatedWorkEffortView(
            associatedWorkEffort.getId(),
            associatedWorkEffort.getWorkEffortId(),
            associatedWorkEffort.getTenantCode(),
            associatedWorkEffort.getEffortNumber(),
            associatedWorkEffort.getAssociatedRecordId(),
            associatedWorkEffort.getAssociatedRecordType()
        );
    }

    private WorkOrderItemFulfillmentView toWorkOrderItemFulfillmentView(WorkOrderItemFulfillment fulfillment) {
        return new WorkOrderItemFulfillmentView(
            fulfillment.getId(),
            fulfillment.getWorkEffortId(),
            fulfillment.getTenantCode(),
            fulfillment.getEffortNumber(),
            fulfillment.getOrderLineItemId(),
            fulfillment.getDescription(),
            fulfillment.getCreatedAt()
        );
    }

    private OrderRequirementCommitmentView toOrderRequirementCommitmentView(OrderRequirementCommitment commitment) {
        return new OrderRequirementCommitmentView(
            commitment.getId(),
            commitment.getOrderLineItemId(),
            commitment.getRequirementId(),
            commitment.getDescription(),
            commitment.getQuantity(),
            commitment.getCreatedAt()
        );
    }

    private WorkRequirementFulfillmentView toWorkRequirementFulfillmentView(WorkRequirementFulfillment fulfillment) {
        return new WorkRequirementFulfillmentView(
            fulfillment.getId(),
            fulfillment.getWorkEffortId(),
            fulfillment.getTenantCode(),
            fulfillment.getEffortNumber(),
            fulfillment.getRequirementId(),
            fulfillment.getDescription(),
            fulfillment.getCreatedAt()
        );
    }

    private WorkEffortAssociationTypeView toAssociationTypeView(WorkEffortAssociationType type) {
        return new WorkEffortAssociationTypeView(
            type.getId(),
            type.getCode(),
            type.getName(),
            type.getDescription(),
            type.getParentTypeCode(),
            type.getValidFromRoleTypeCode(),
            type.getValidToRoleTypeCode(),
            type.getExternalIdentifier(),
            type.getExternalIdSource(),
            type.getCreatedAt()
        );
    }

    private WorkEffortAssociationView toAssociationView(WorkEffortAssociation association) {
        return new WorkEffortAssociationView(
            association.getId(),
            association.getTenantCode(),
            association.getAssociationTypeCode(),
            association.getDescription(),
            association.getFromWorkEffortId(),
            association.getFromEffortNumber(),
            association.getToWorkEffortId(),
            association.getToEffortNumber(),
            association.getFromRoleTypeCode(),
            association.getToRoleTypeCode(),
            association.getRelationshipTypeCode(),
            association.getEffectiveFrom(),
            association.getEffectiveThru(),
            association.getCreatedAt()
        );
    }

    private WorkEffortFixedAssetAssignmentView toFixedAssetAssignmentView(
        WorkEffortFixedAssetAssignment assignment
    ) {
        return new WorkEffortFixedAssetAssignmentView(
            assignment.getId(),
            assignment.getWorkEffortId(),
            assignment.getTenantCode(),
            assignment.getEffortNumber(),
            assignment.getFixedAssetCode(),
            assignment.getCreatedAt()
        );
    }

    private WorkEffortInventoryAssignmentView toInventoryAssignmentView(
        WorkEffortInventoryAssignment assignment
    ) {
        return new WorkEffortInventoryAssignmentView(
            assignment.getId(),
            assignment.getWorkEffortId(),
            assignment.getTenantCode(),
            assignment.getEffortNumber(),
            assignment.getInventoryEntryCode(),
            assignment.getCreatedAt()
        );
    }

    private WorkEffortPartyAssignmentView toPartyAssignmentView(
        WorkEffortPartyAssignment assignment
    ) {
        return new WorkEffortPartyAssignmentView(
            assignment.getId(),
            assignment.getWorkEffortId(),
            assignment.getTenantCode(),
            assignment.getEffortNumber(),
            assignment.getPartyCode(),
            assignment.getRoleTypeCode(),
            assignment.getAssignedFrom(),
            assignment.getAssignedThru(),
            assignment.getComments(),
            assignment.getCreatedAt()
        );
    }

    private WorkEffortRoleTypeAssignmentView toRoleTypeAssignmentView(
        WorkEffortRoleTypeAssignment assignment
    ) {
        return new WorkEffortRoleTypeAssignmentView(
            assignment.getId(),
            assignment.getWorkEffortId(),
            assignment.getTenantCode(),
            assignment.getEffortNumber(),
            assignment.getRoleTypeCode()
        );
    }

    private WorkEffort findWorkEffort(String tenantCode, String effortNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        return workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAssignmentActivityByBucket(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery,
        AssignmentBucketExtractor<B> bucketExtractor,
        AssignmentBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        List<WorkEffortAssignmentChangeAudit> audits = workEffortAssignmentChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo
        );
        TreeMap<B, AssignmentBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (WorkEffortAssignmentChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            AssignmentBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new AssignmentBucketSummary());
            summary.assignmentCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey(),
                entry.getValue().assignmentCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAssignmentActivityByBucketAndAssignee(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery,
        AssignmentBucketExtractor<B> bucketExtractor,
        AssignmentBucketAssigneeSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        List<WorkEffortAssignmentChangeAudit> audits = workEffortAssignmentChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo
        );
        Map<AssignmentBucketAssigneeKey<B>, AssignmentBucketSummary> summaries = new java.util.HashMap<>();
        for (WorkEffortAssignmentChangeAudit audit : audits) {
            AssignmentBucketAssigneeKey<B> key = new AssignmentBucketAssigneeKey<>(
                bucketExtractor.bucket(audit),
                audit.getCurrentAssignedTo()
            );
            AssignmentBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new AssignmentBucketSummary());
            summary.assignmentCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().assignedTo().compareTo(right.getKey().assignedTo());
            })
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey().bucket(),
                entry.getKey().assignedTo(),
                entry.getValue().assignmentCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucket(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<WorkEffortStatusChangeAudit> audits = workEffortStatusChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        TreeMap<B, StatusBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (WorkEffortStatusChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            StatusBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey(),
                entry.getValue().transitionCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucketAndCurrentStatus(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketStatusSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<WorkEffortStatusChangeAudit> audits = workEffortStatusChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        Map<StatusBucketStatusKey<B>, StatusBucketSummary> summaries = new java.util.HashMap<>();
        for (WorkEffortStatusChangeAudit audit : audits) {
            StatusBucketStatusKey<B> key = new StatusBucketStatusKey<>(bucketExtractor.bucket(audit), audit.getCurrentStatus());
            StatusBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().currentStatus().compareTo(right.getKey().currentStatus());
            })
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey().bucket(),
                entry.getKey().currentStatus(),
                entry.getValue().transitionCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private static <T> PageResult<T> paginate(List<T> rows, PageQuery pageQuery) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), rows.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), rows.size());
        List<T> pageRows = new ArrayList<>(rows.subList(fromIndex, toIndex));
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / pageQuery.size());
        return new PageResult<>(
            pageRows,
            pageQuery.page(),
            pageQuery.size(),
            rows.size(),
            totalPages,
            pageQuery.page() + 1 < totalPages,
            pageQuery.page() > 0 && !rows.isEmpty()
        );
    }

    @FunctionalInterface
    private interface AssignmentBucketExtractor<B> {
        B bucket(WorkEffortAssignmentChangeAudit audit);
    }

    @FunctionalInterface
    private interface AssignmentBucketSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, long assignmentCount, long workEffortCount);
    }

    @FunctionalInterface
    private interface AssignmentBucketAssigneeSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, String assignedTo, long assignmentCount, long workEffortCount);
    }

    private record AssignmentBucketAssigneeKey<B extends Comparable<? super B>>(B bucket, String assignedTo) {
    }

    @FunctionalInterface
    private interface StatusBucketExtractor<B> {
        B bucket(WorkEffortStatusChangeAudit audit);
    }

    @FunctionalInterface
    private interface StatusBucketSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, long transitionCount, long workEffortCount);
    }

    @FunctionalInterface
    private interface StatusBucketStatusSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, WorkEffortStatus currentStatus, long transitionCount, long workEffortCount);
    }

    private record StatusBucketStatusKey<B extends Comparable<? super B>>(B bucket, WorkEffortStatus currentStatus) {
    }

    private static final class AssignmentBucketSummary {
        private long assignmentCount;
        private final Set<UUID> workEffortIds = new HashSet<>();
    }

    private static final class StatusBucketSummary {
        private long transitionCount;
        private final Set<UUID> workEffortIds = new HashSet<>();
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

    private static String normalizeOptional(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName);
    }

    private static String normalizeAssignedTo(String assignedTo) {
        return normalizeActorEmail(assignedTo, "assignedTo");
    }

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
