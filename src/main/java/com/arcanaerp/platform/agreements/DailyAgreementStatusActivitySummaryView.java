package com.arcanaerp.platform.agreements;

import java.time.LocalDate;

public record DailyAgreementStatusActivitySummaryView(
    LocalDate businessDate,
    long transitionCount,
    long agreementCount
) {
}
