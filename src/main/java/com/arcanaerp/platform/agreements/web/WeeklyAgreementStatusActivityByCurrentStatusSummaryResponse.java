package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.LocalDate;

public record WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessWeekStart,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
