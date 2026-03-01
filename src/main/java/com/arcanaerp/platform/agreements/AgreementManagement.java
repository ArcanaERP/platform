package com.arcanaerp.platform.agreements;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface AgreementManagement {

    AgreementView createAgreement(CreateAgreementCommand command);

    AgreementView getAgreement(String agreementNumber);

    PageResult<AgreementView> listAgreements(PageQuery pageQuery, AgreementStatus status);

    AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command);
}
