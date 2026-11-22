package com.fairfold.resume;

import com.fairfold.common.errors.ApiException;

import java.time.LocalDate;
import java.time.YearMonth;

public final class MonthFormat {

    private MonthFormat() {
    }

    /** Accepts YYYY-MM, YYYY-MM-DD or YYYY and returns the first day. */
    public static LocalDate parse(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.strip();
        try {
            if (clean.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(clean), 1, 1);
            }
            if (clean.matches("\\d{4}-\\d{2}")) {
                return YearMonth.parse(clean).atDay(1);
            }
            if (clean.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(clean);
            }
        } catch (java.time.DateTimeException e) {
            throw ApiException.badRequest("INVALID_DATE", field + " must be a valid month.");
        }
        throw ApiException.badRequest("INVALID_DATE", field + " must be a month like 2024-03.");
    }

    public static String format(LocalDate date) {
        return date == null ? null : YearMonth.from(date).toString();
    }
}
