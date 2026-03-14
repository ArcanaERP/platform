package com.arcanaerp.platform.payments;

public enum ReceivablesAgingBucket {
    CURRENT,
    OVERDUE_1_TO_30,
    OVERDUE_31_TO_60,
    OVERDUE_61_TO_90,
    OVERDUE_OVER_90;

    public static ReceivablesAgingBucket fromDaysPastDue(long daysPastDue) {
        if (daysPastDue <= 0) {
            return CURRENT;
        }
        if (daysPastDue <= 30) {
            return OVERDUE_1_TO_30;
        }
        if (daysPastDue <= 60) {
            return OVERDUE_31_TO_60;
        }
        if (daysPastDue <= 90) {
            return OVERDUE_61_TO_90;
        }
        return OVERDUE_OVER_90;
    }
}
