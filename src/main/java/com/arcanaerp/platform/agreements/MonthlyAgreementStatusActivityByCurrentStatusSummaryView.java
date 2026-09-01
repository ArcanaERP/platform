package com.arcanaerp.platform.agreements;

import java.time.YearMonth;

public record MonthlyAgreementStatusActivityByCurrentStatusSummaryView(
    YearMonth businessMonth,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
