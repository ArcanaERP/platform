package com.arcanaerp.platform.agreements;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface AgreementManagement {

    AgreementView createAgreement(CreateAgreementCommand command);

    AgreementView getAgreement(String agreementNumber);

    PageResult<AgreementView> listAgreements(PageQuery pageQuery, AgreementStatus status);

    AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command);

    PageResult<AgreementStatusChangeView> listStatusHistory(
        String agreementNumber,
        String tenantCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
