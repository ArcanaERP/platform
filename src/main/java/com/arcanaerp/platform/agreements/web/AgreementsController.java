package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementItemTypeView;
import com.arcanaerp.platform.agreements.AgreementItemView;
import com.arcanaerp.platform.agreements.AgreementPartyRoleView;
import com.arcanaerp.platform.agreements.AgreementRelationshipTypeView;
import com.arcanaerp.platform.agreements.AgreementRelationshipView;
import com.arcanaerp.platform.agreements.AgreementRoleTypeView;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementStatusChangeView;
import com.arcanaerp.platform.agreements.AgreementTypeView;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.LoyaltyProgramCodeView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.RegisterAgreementItemCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementItemTypeCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementPartyRoleCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementRelationshipCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementRelationshipTypeCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementRoleTypeCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementTypeCommand;
import com.arcanaerp.platform.agreements.RegisterLoyaltyProgramCodeCommand;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
public class AgreementsController {

    private final AgreementManagement agreementManagement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementResponse createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        AgreementView agreement = agreementManagement.createAgreement(
            new CreateAgreementCommand(
                request.tenantCode(),
                request.agreementNumber(),
                request.name(),
                request.agreementType(),
                request.effectiveFrom(),
                request.description(),
                request.agreementTypeId(),
                request.agreementStatus(),
                request.productId(),
                request.agreementDate(),
                request.fromDate(),
                request.thruDate(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        );
        return toResponse(agreement);
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementTypeResponse createAgreementType(@Valid @RequestBody CreateAgreementTypeRequest request) {
        return toAgreementTypeResponse(agreementManagement.registerAgreementType(
            new RegisterAgreementTypeCommand(
                request.parentId(),
                request.left(),
                request.right(),
                request.description(),
                request.comments(),
                request.internalIdentifier(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/types/{id}")
    public AgreementTypeResponse agreementTypeById(@PathVariable UUID id) {
        return toAgreementTypeResponse(agreementManagement.agreementTypeById(id));
    }

    @GetMapping("/types")
    public PageResult<AgreementTypeResponse> listAgreementTypes(
        @RequestParam(required = false) Long parentId,
        @RequestParam(required = false) String internalIdentifier,
        @RequestParam(required = false) String externalIdentifier,
        @RequestParam(required = false) String externalIdSource,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementTypes(
            parentId,
            internalIdentifier,
            externalIdentifier,
            externalIdSource,
            PageQuery.of(page, size)
        ).map(this::toAgreementTypeResponse);
    }

    @PostMapping("/item-types")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementItemTypeResponse createAgreementItemType(
        @Valid @RequestBody CreateAgreementItemTypeRequest request
    ) {
        return toAgreementItemTypeResponse(agreementManagement.registerAgreementItemType(
            new RegisterAgreementItemTypeCommand(
                request.parentId(),
                request.left(),
                request.right(),
                request.description(),
                request.comments(),
                request.internalIdentifier(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/item-types/{id}")
    public AgreementItemTypeResponse agreementItemTypeById(@PathVariable UUID id) {
        return toAgreementItemTypeResponse(agreementManagement.agreementItemTypeById(id));
    }

    @GetMapping("/item-types")
    public PageResult<AgreementItemTypeResponse> listAgreementItemTypes(
        @RequestParam(required = false) Long parentId,
        @RequestParam(required = false) String internalIdentifier,
        @RequestParam(required = false) String externalIdentifier,
        @RequestParam(required = false) String externalIdSource,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementItemTypes(
            parentId,
            internalIdentifier,
            externalIdentifier,
            externalIdSource,
            PageQuery.of(page, size)
        ).map(this::toAgreementItemTypeResponse);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementItemResponse createAgreementItem(@Valid @RequestBody CreateAgreementItemRequest request) {
        return toAgreementItemResponse(agreementManagement.registerAgreementItem(
            new RegisterAgreementItemCommand(
                request.agreementId(),
                request.agreementItemTypeId(),
                request.agreementItemValue(),
                request.description(),
                request.agreementItemRuleString()
            )
        ));
    }

    @GetMapping("/items/{id}")
    public AgreementItemResponse agreementItemById(@PathVariable UUID id) {
        return toAgreementItemResponse(agreementManagement.agreementItemById(id));
    }

    @GetMapping("/{agreementNumber}/items/by-type/{itemTypeInternalIdentifier}")
    public AgreementItemResponse agreementItemByTypeInternalIdentifier(
        @PathVariable String agreementNumber,
        @PathVariable String itemTypeInternalIdentifier
    ) {
        return toAgreementItemResponse(agreementManagement.agreementItemByTypeInternalIdentifier(
            agreementNumber,
            itemTypeInternalIdentifier
        ));
    }

    @GetMapping("/items")
    public PageResult<AgreementItemResponse> listAgreementItems(
        @RequestParam(required = false) UUID agreementId,
        @RequestParam(required = false) UUID agreementItemTypeId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementItems(
            agreementId,
            agreementItemTypeId,
            PageQuery.of(page, size)
        ).map(this::toAgreementItemResponse);
    }

    @PostMapping("/role-types")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementRoleTypeResponse createAgreementRoleType(
        @Valid @RequestBody CreateAgreementRoleTypeRequest request
    ) {
        return toAgreementRoleTypeResponse(agreementManagement.registerAgreementRoleType(
            new RegisterAgreementRoleTypeCommand(
                request.parentId(),
                request.left(),
                request.right(),
                request.description(),
                request.comments(),
                request.internalIdentifier(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/role-types/{id}")
    public AgreementRoleTypeResponse agreementRoleTypeById(@PathVariable UUID id) {
        return toAgreementRoleTypeResponse(agreementManagement.agreementRoleTypeById(id));
    }

    @GetMapping("/role-types")
    public PageResult<AgreementRoleTypeResponse> listAgreementRoleTypes(
        @RequestParam(required = false) Long parentId,
        @RequestParam(required = false) String internalIdentifier,
        @RequestParam(required = false) String externalIdentifier,
        @RequestParam(required = false) String externalIdSource,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementRoleTypes(
            parentId,
            internalIdentifier,
            externalIdentifier,
            externalIdSource,
            PageQuery.of(page, size)
        ).map(this::toAgreementRoleTypeResponse);
    }

    @PostMapping("/party-roles")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementPartyRoleResponse createAgreementPartyRole(
        @Valid @RequestBody CreateAgreementPartyRoleRequest request
    ) {
        return toAgreementPartyRoleResponse(agreementManagement.registerAgreementPartyRole(
            new RegisterAgreementPartyRoleCommand(
                request.description(),
                request.agreementId(),
                request.partyId(),
                request.roleTypeId(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/party-roles/{id}")
    public AgreementPartyRoleResponse agreementPartyRoleById(@PathVariable UUID id) {
        return toAgreementPartyRoleResponse(agreementManagement.agreementPartyRoleById(id));
    }

    @GetMapping("/party-roles")
    public PageResult<AgreementPartyRoleResponse> listAgreementPartyRoles(
        @RequestParam(required = false) UUID agreementId,
        @RequestParam(required = false) Long partyId,
        @RequestParam(required = false) Long roleTypeId,
        @RequestParam(required = false) String externalIdentifier,
        @RequestParam(required = false) String externalIdSource,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementPartyRoles(
            agreementId,
            partyId,
            roleTypeId,
            externalIdentifier,
            externalIdSource,
            PageQuery.of(page, size)
        ).map(this::toAgreementPartyRoleResponse);
    }

    @PostMapping("/relationship-types")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementRelationshipTypeResponse createAgreementRelationshipType(
        @Valid @RequestBody CreateAgreementRelationshipTypeRequest request
    ) {
        return toAgreementRelationshipTypeResponse(agreementManagement.registerAgreementRelationshipType(
            new RegisterAgreementRelationshipTypeCommand(
                request.parentId(),
                request.left(),
                request.right(),
                request.validFromRoleTypeId(),
                request.validToRoleTypeId(),
                request.name(),
                request.description(),
                request.internalIdentifier(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/relationship-types/{id}")
    public AgreementRelationshipTypeResponse agreementRelationshipTypeById(@PathVariable UUID id) {
        return toAgreementRelationshipTypeResponse(agreementManagement.agreementRelationshipTypeById(id));
    }

    @GetMapping("/relationship-types")
    public PageResult<AgreementRelationshipTypeResponse> listAgreementRelationshipTypes(
        @RequestParam(required = false) Long parentId,
        @RequestParam(required = false) UUID validFromRoleTypeId,
        @RequestParam(required = false) UUID validToRoleTypeId,
        @RequestParam(required = false) String internalIdentifier,
        @RequestParam(required = false) String externalIdentifier,
        @RequestParam(required = false) String externalIdSource,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementRelationshipTypes(
            parentId,
            validFromRoleTypeId,
            validToRoleTypeId,
            internalIdentifier,
            externalIdentifier,
            externalIdSource,
            PageQuery.of(page, size)
        ).map(this::toAgreementRelationshipTypeResponse);
    }

    @PostMapping("/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementRelationshipResponse createAgreementRelationship(
        @Valid @RequestBody CreateAgreementRelationshipRequest request
    ) {
        return toAgreementRelationshipResponse(agreementManagement.registerAgreementRelationship(
            new RegisterAgreementRelationshipCommand(
                request.agreementRelationshipTypeId(),
                request.description(),
                request.agreementIdFrom(),
                request.agreementIdTo(),
                request.roleTypeIdFrom(),
                request.roleTypeIdTo(),
                request.statusTypeId(),
                request.fromDate(),
                request.thruDate()
            )
        ));
    }

    @GetMapping("/relationships/{id}")
    public AgreementRelationshipResponse agreementRelationshipById(@PathVariable UUID id) {
        return toAgreementRelationshipResponse(agreementManagement.agreementRelationshipById(id));
    }

    @GetMapping("/{agreementNumber}/relationships")
    public PageResult<AgreementRelationshipResponse> listAgreementRelationshipsForAgreement(
        @PathVariable String agreementNumber,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementRelationshipsForAgreement(
            agreementNumber,
            PageQuery.of(page, size)
        ).map(this::toAgreementRelationshipResponse);
    }

    @GetMapping("/relationships")
    public PageResult<AgreementRelationshipResponse> listAgreementRelationships(
        @RequestParam(required = false) UUID agreementRelationshipTypeId,
        @RequestParam(required = false) UUID agreementIdFrom,
        @RequestParam(required = false) UUID agreementIdTo,
        @RequestParam(required = false) UUID roleTypeIdFrom,
        @RequestParam(required = false) UUID roleTypeIdTo,
        @RequestParam(required = false) Long statusTypeId,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate thruDate,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listAgreementRelationships(
            agreementRelationshipTypeId,
            agreementIdFrom,
            agreementIdTo,
            roleTypeIdFrom,
            roleTypeIdTo,
            statusTypeId,
            fromDate,
            thruDate,
            PageQuery.of(page, size)
        ).map(this::toAgreementRelationshipResponse);
    }

    @PostMapping("/loyalty-program-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoyaltyProgramCodeResponse createLoyaltyProgramCode(
        @Valid @RequestBody CreateLoyaltyProgramCodeRequest request
    ) {
        return toLoyaltyProgramCodeResponse(agreementManagement.registerLoyaltyProgramCode(
            new RegisterLoyaltyProgramCodeCommand(
                request.identifier(),
                request.name(),
                request.description()
            )
        ));
    }

    @GetMapping("/loyalty-program-codes/{id}")
    public LoyaltyProgramCodeResponse loyaltyProgramCodeById(@PathVariable UUID id) {
        return toLoyaltyProgramCodeResponse(agreementManagement.loyaltyProgramCodeById(id));
    }

    @GetMapping("/loyalty-program-codes")
    public PageResult<LoyaltyProgramCodeResponse> listLoyaltyProgramCodes(
        @RequestParam(required = false) String identifier,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listLoyaltyProgramCodes(
            identifier,
            name,
            PageQuery.of(page, size)
        ).map(this::toLoyaltyProgramCodeResponse);
    }

    @GetMapping("/{agreementNumber}")
    public AgreementResponse getAgreement(@PathVariable String agreementNumber) {
        return toResponse(agreementManagement.getAgreement(agreementNumber));
    }

    @GetMapping
    public PageResult<AgreementResponse> listAgreements(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String status
    ) {
        return agreementManagement.listAgreements(
            normalizeOptionalTenantCode(tenantCode),
            PageQuery.of(page, size),
            parseOptionalStatus(status)
        ).map(this::toResponse);
    }

    @GetMapping("/status-activity/daily-summary")
    public PageResult<DailyAgreementStatusActivitySummaryResponse> listDailyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listDailyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/daily-summary/by-current-status")
    public PageResult<DailyAgreementStatusActivityByCurrentStatusSummaryResponse> listDailyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listDailyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary")
    public PageResult<WeeklyAgreementStatusActivitySummaryResponse> listWeeklyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listWeeklyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary/by-current-status")
    public PageResult<WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse> listWeeklyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listWeeklyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary")
    public PageResult<MonthlyAgreementStatusActivitySummaryResponse> listMonthlyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listMonthlyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary/by-current-status")
    public PageResult<MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse> listMonthlyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listMonthlyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/{agreementNumber}/status-history")
    public PageResult<AgreementStatusChangeResponse> listStatusHistory(
        @PathVariable String agreementNumber,
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listStatusHistory(
                agreementNumber,
                normalizeOptionalTenantCode(tenantCode),
                normalizeOptionalChangedBy(changedBy),
                parsedChangedAtFrom,
                parsedChangedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toStatusHistoryResponse);
    }

    @PatchMapping("/{agreementNumber}/status")
    public AgreementResponse changeAgreementStatus(
        @PathVariable String agreementNumber,
        @Valid @RequestBody ChangeAgreementStatusRequest request
    ) {
        AgreementView agreement = agreementManagement.changeAgreementStatus(
            new ChangeAgreementStatusCommand(
                agreementNumber,
                request.status(),
                request.tenantCode(),
                request.reason(),
                request.changedBy()
            )
        );
        return toResponse(agreement);
    }

    private AgreementResponse toResponse(AgreementView agreement) {
        return new AgreementResponse(
                agreement.id(),
                agreement.tenantCode(),
                agreement.agreementNumber(),
                agreement.name(),
                agreement.agreementType(),
                agreement.status(),
                agreement.effectiveFrom(),
                agreement.createdAt(),
                agreement.activatedAt(),
                agreement.terminatedAt(),
                agreement.description(),
                agreement.agreementTypeId(),
                agreement.agreementStatus(),
                agreement.productId(),
                agreement.agreementDate(),
                agreement.fromDate(),
                agreement.thruDate(),
                agreement.externalIdentifier(),
                agreement.externalIdSource()
            );
    }

    private AgreementTypeResponse toAgreementTypeResponse(AgreementTypeView type) {
        return new AgreementTypeResponse(
            type.id(),
            type.parentId(),
            type.left(),
            type.right(),
            type.description(),
            type.comments(),
            type.internalIdentifier(),
            type.externalIdentifier(),
            type.externalIdSource(),
            type.createdAt()
        );
    }

    private AgreementItemTypeResponse toAgreementItemTypeResponse(AgreementItemTypeView type) {
        return new AgreementItemTypeResponse(
            type.id(),
            type.parentId(),
            type.left(),
            type.right(),
            type.description(),
            type.comments(),
            type.internalIdentifier(),
            type.externalIdentifier(),
            type.externalIdSource(),
            type.createdAt()
        );
    }

    private AgreementItemResponse toAgreementItemResponse(AgreementItemView item) {
        return new AgreementItemResponse(
            item.id(),
            item.agreementId(),
            item.agreementItemTypeId(),
            item.agreementItemValue(),
            item.description(),
            item.agreementItemRuleString(),
            item.createdAt()
        );
    }

    private AgreementRoleTypeResponse toAgreementRoleTypeResponse(AgreementRoleTypeView type) {
        return new AgreementRoleTypeResponse(
            type.id(),
            type.parentId(),
            type.left(),
            type.right(),
            type.description(),
            type.comments(),
            type.internalIdentifier(),
            type.externalIdentifier(),
            type.externalIdSource(),
            type.createdAt()
        );
    }

    private AgreementPartyRoleResponse toAgreementPartyRoleResponse(AgreementPartyRoleView partyRole) {
        return new AgreementPartyRoleResponse(
            partyRole.id(),
            partyRole.description(),
            partyRole.agreementId(),
            partyRole.partyId(),
            partyRole.roleTypeId(),
            partyRole.externalIdentifier(),
            partyRole.externalIdSource(),
            partyRole.createdAt()
        );
    }

    private AgreementRelationshipTypeResponse toAgreementRelationshipTypeResponse(
        AgreementRelationshipTypeView type
    ) {
        return new AgreementRelationshipTypeResponse(
            type.id(),
            type.parentId(),
            type.left(),
            type.right(),
            type.validFromRoleTypeId(),
            type.validToRoleTypeId(),
            type.name(),
            type.description(),
            type.internalIdentifier(),
            type.externalIdentifier(),
            type.externalIdSource(),
            type.createdAt()
        );
    }

    private AgreementRelationshipResponse toAgreementRelationshipResponse(AgreementRelationshipView relationship) {
        return new AgreementRelationshipResponse(
            relationship.id(),
            relationship.agreementRelationshipTypeId(),
            relationship.description(),
            relationship.agreementIdFrom(),
            relationship.agreementIdTo(),
            relationship.roleTypeIdFrom(),
            relationship.roleTypeIdTo(),
            relationship.statusTypeId(),
            relationship.fromDate(),
            relationship.thruDate(),
            relationship.createdAt()
        );
    }

    private LoyaltyProgramCodeResponse toLoyaltyProgramCodeResponse(LoyaltyProgramCodeView code) {
        return new LoyaltyProgramCodeResponse(
            code.id(),
            code.identifier(),
            code.name(),
            code.description(),
            code.createdAt()
        );
    }

    private AgreementStatusChangeResponse toStatusHistoryResponse(AgreementStatusChangeView change) {
        return new AgreementStatusChangeResponse(
            change.id(),
            change.agreementNumber(),
            change.previousStatus(),
            change.currentStatus(),
            change.tenantCode(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private DailyAgreementStatusActivitySummaryResponse toDailyStatusActivitySummaryResponse(
        DailyAgreementStatusActivitySummaryView view
    ) {
        return new DailyAgreementStatusActivitySummaryResponse(
            view.businessDate(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private DailyAgreementStatusActivityByCurrentStatusSummaryResponse toDailyStatusActivityByCurrentStatusSummaryResponse(
        DailyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new DailyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessDate(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private WeeklyAgreementStatusActivitySummaryResponse toWeeklyStatusActivitySummaryResponse(
        WeeklyAgreementStatusActivitySummaryView view
    ) {
        return new WeeklyAgreementStatusActivitySummaryResponse(
            view.businessWeekStart(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse toWeeklyStatusActivityByCurrentStatusSummaryResponse(
        WeeklyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessWeekStart(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private MonthlyAgreementStatusActivitySummaryResponse toMonthlyStatusActivitySummaryResponse(
        MonthlyAgreementStatusActivitySummaryView view
    ) {
        return new MonthlyAgreementStatusActivitySummaryResponse(
            view.businessMonth(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse toMonthlyStatusActivityByCurrentStatusSummaryResponse(
        MonthlyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessMonth(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private static AgreementStatus parseOptionalStatus(String status) {
        return parseOptionalStatus(status, "status");
    }

    private static AgreementStatus parseOptionalStatus(String status, String parameterName) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        String normalized = status.trim().toUpperCase();
        try {
            return AgreementStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be one of: DRAFT, ACTIVE, TERMINATED");
        }
    }

    private static String normalizeOptionalTenantCode(String tenantCode) {
        if (tenantCode == null) {
            return null;
        }
        if (tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode query parameter must not be blank");
        }
        return tenantCode.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        return changedBy.trim().toLowerCase();
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
