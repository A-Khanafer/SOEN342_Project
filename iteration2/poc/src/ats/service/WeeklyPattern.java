package ats.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class WeeklyPattern implements RecurrencePattern {

    private final Set<DayOfWeek> days;

    public WeeklyPattern(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one day of the week");
        }
        this.days = EnumSet.copyOf(days);
    }

    @Override
    public boolean includes(LocalDate date) {
        return days.contains(date.getDayOfWeek());
    }

    @Override
    public String toString() {
        return "every " + days;
    }
}
