package com.arcanaerp.platform.agreements;

import java.time.LocalDate;

public record WeeklyAgreementStatusActivityByCurrentStatusSummaryView(
    LocalDate businessWeekStart,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
