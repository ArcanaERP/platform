package com.arcanaerp.platform.agreements.web;

import java.time.LocalDate;

public record WeeklyAgreementStatusActivitySummaryResponse(
    LocalDate businessWeekStart,
    long transitionCount,
    long agreementCount
) {
}
