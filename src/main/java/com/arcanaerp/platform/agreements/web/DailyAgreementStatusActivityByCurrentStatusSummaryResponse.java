package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.LocalDate;

public record DailyAgreementStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessDate,
    AgreementStatus currentStatus,
    long transitionCount,
    long agreementCount
) {
}
