package com.arcanaerp.platform.orders.web;

import java.time.LocalDate;

public record DailyOrderStatusActivitySummaryResponse(
    LocalDate businessDate,
    long transitionCount,
    long orderCount
) {
}
