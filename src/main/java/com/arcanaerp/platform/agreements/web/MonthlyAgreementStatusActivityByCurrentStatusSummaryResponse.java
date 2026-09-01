package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.YearMonth;

public record MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse(
    YearMonth businessMonth,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
