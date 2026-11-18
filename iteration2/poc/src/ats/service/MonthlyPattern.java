package ats.service;

import java.time.LocalDate;

public class MonthlyPattern implements RecurrencePattern {

    private final int dayOfMonth;

    public MonthlyPattern(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("Day of month must be between 1 and 31");
        }
        this.dayOfMonth = dayOfMonth;
    }

    @Override
    public boolean includes(LocalDate date) {
        return date.getDayOfMonth() == dayOfMonth;
    }

    @Override
    public String toString() {
        return "day " + dayOfMonth + " of each month";
    }
}
