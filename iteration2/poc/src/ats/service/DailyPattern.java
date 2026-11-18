package ats.service;

import java.time.LocalDate;

public class DailyPattern implements RecurrencePattern {

    @Override
    public boolean includes(LocalDate date) {
        return true;
    }

    @Override
    public String toString() {
        return "every day";
    }
}