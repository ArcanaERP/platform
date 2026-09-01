package com.arcanaerp.platform.agreements;

import java.time.LocalDate;

public record WeeklyAgreementStatusActivitySummaryView(
    LocalDate businessWeekStart,
    long transitionCount,
    long agreementCount
) {
}
