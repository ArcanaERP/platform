package com.arcanaerp.platform.agreements;

import java.time.YearMonth;

public record MonthlyAgreementStatusActivitySummaryView(
    YearMonth businessMonth,
    long transitionCount,
    long agreementCount
) {
}
