package com.arcanaerp.platform.agreements;

import java.time.LocalDate;

public record DailyAgreementStatusActivityByCurrentStatusSummaryView(
    LocalDate businessDate,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
