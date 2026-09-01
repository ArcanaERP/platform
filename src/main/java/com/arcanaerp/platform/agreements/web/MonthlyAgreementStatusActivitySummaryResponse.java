package com.arcanaerp.platform.agreements.web;

import java.time.YearMonth;

public record MonthlyAgreementStatusActivitySummaryResponse(
    YearMonth businessMonth,
    long transitionCount,
    long agreementCount
) {
}
