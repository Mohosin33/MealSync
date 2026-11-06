package com.example.mealsync.close;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class PeriodDates {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PeriodDates() {
    }

    public static String getFirstDay(String periodId) {
        YearMonth ym = YearMonth.parse(periodId, PERIOD_FORMATTER);
        return ym.atDay(1).format(DATE_FORMATTER);
    }

    public static String getLastDay(String periodId) {
        YearMonth ym = YearMonth.parse(periodId, PERIOD_FORMATTER);
        return ym.atEndOfMonth().format(DATE_FORMATTER);
    }

    public static String getNextPeriodId(String periodId) {
        YearMonth ym = YearMonth.parse(periodId, PERIOD_FORMATTER);
        return ym.plusMonths(1).format(PERIOD_FORMATTER);
    }

    public static boolean canClose(String todayLocalDateStr, String periodEndDateStr) {
        if (todayLocalDateStr == null || periodEndDateStr == null) {
            return false;
        }
        return todayLocalDateStr.trim().compareTo(periodEndDateStr.trim()) >= 0;
    }
}
