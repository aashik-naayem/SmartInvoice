package com.smartinvoice.util;

import com.smartinvoice.enums.RecurrenceFrequency;

import java.time.LocalDate;

/**
 * Advances a recurring schedule's run date by one period. Shared by the scheduler (advancing
 * after a successful generation) and the service layer (catching a paused schedule back up to
 * the next future occurrence on resume).
 */
public final class RecurrenceCalculator {

    private RecurrenceCalculator() {
    }

    public static LocalDate next(LocalDate from, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case YEARLY -> from.plusYears(1);
        };
    }
}
