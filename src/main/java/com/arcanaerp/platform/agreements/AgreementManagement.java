package com.arcanaerp.platform.agreements;

public interface AgreementManagement {

    AgreementView createAgreement(CreateAgreementCommand command);

    AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command);
}
