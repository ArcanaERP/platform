package com.arcanaerp.platform.agreements.web;

import java.time.LocalDate;

public record DailyAgreementStatusActivitySummaryResponse(
    LocalDate businessDate,
    long transitionCount,
    long agreementCount
) {
}
