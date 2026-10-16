package com.arcanaerp.platform.agreements;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface AgreementManagement {

    AgreementView createAgreement(CreateAgreementCommand command);

    AgreementTypeView registerAgreementType(RegisterAgreementTypeCommand command);

    AgreementTypeView agreementTypeById(UUID id);

    PageResult<AgreementTypeView> listAgreementTypes(
        Long parentId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    );

    AgreementItemTypeView registerAgreementItemType(RegisterAgreementItemTypeCommand command);

    AgreementItemTypeView agreementItemTypeById(UUID id);

    PageResult<AgreementItemTypeView> listAgreementItemTypes(
        Long parentId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    );

    AgreementItemView registerAgreementItem(RegisterAgreementItemCommand command);

    AgreementItemView agreementItemById(UUID id);

    AgreementItemView agreementItemByTypeInternalIdentifier(
        String agreementNumber,
        String itemTypeInternalIdentifier
    );

    PageResult<AgreementItemView> listAgreementItems(
        UUID agreementId,
        UUID agreementItemTypeId,
        PageQuery pageQuery
    );

    AgreementRoleTypeView registerAgreementRoleType(RegisterAgreementRoleTypeCommand command);

    AgreementRoleTypeView agreementRoleTypeById(UUID id);

    PageResult<AgreementRoleTypeView> listAgreementRoleTypes(
        Long parentId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    );

    AgreementPartyRoleView registerAgreementPartyRole(RegisterAgreementPartyRoleCommand command);

    AgreementPartyRoleView agreementPartyRoleById(UUID id);

    PageResult<AgreementPartyRoleView> listAgreementPartyRoles(
        UUID agreementId,
        Long partyId,
        Long roleTypeId,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    );

    AgreementRelationshipTypeView registerAgreementRelationshipType(
        RegisterAgreementRelationshipTypeCommand command
    );

    AgreementRelationshipTypeView agreementRelationshipTypeById(UUID id);

    PageResult<AgreementRelationshipTypeView> listAgreementRelationshipTypes(
        Long parentId,
        UUID validFromRoleTypeId,
        UUID validToRoleTypeId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    );

    AgreementRelationshipView registerAgreementRelationship(RegisterAgreementRelationshipCommand command);

    AgreementRelationshipView agreementRelationshipById(UUID id);

    PageResult<AgreementRelationshipView> listAgreementRelationshipsForAgreement(
        String agreementNumber,
        PageQuery pageQuery
    );

    PageResult<AgreementRelationshipView> listAgreementRelationships(
        UUID agreementRelationshipTypeId,
        UUID agreementIdFrom,
        UUID agreementIdTo,
        UUID roleTypeIdFrom,
        UUID roleTypeIdTo,
        Long statusTypeId,
        LocalDate fromDate,
        LocalDate thruDate,
        PageQuery pageQuery
    );

    LoyaltyProgramCodeView registerLoyaltyProgramCode(RegisterLoyaltyProgramCodeCommand command);

    LoyaltyProgramCodeView loyaltyProgramCodeById(UUID id);

    PageResult<LoyaltyProgramCodeView> listLoyaltyProgramCodes(
        String identifier,
        String name,
        PageQuery pageQuery
    );

    AgreementView getAgreement(String agreementNumber);

    PageResult<AgreementView> listAgreements(PageQuery pageQuery, AgreementStatus status);

    PageResult<AgreementView> listAgreements(String tenantCode, PageQuery pageQuery, AgreementStatus status);

    AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command);

    PageResult<AgreementStatusChangeView> listStatusHistory(
        String agreementNumber,
        String tenantCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyAgreementStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyAgreementStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyAgreementStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyAgreementStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyAgreementStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyAgreementStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
