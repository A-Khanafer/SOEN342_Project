package ats.service;

import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.TimeSlot;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AvailabilityService {

    private final List<TimeSlot> slots = new ArrayList<>();

    
    public List<TimeSlot> createAvailability(HealthcareProfessional professional, ClinicLocation location,
                                             LocalDate startDate, LocalDate endDate,
                                             RecurrencePattern pattern,
                                             LocalTime startTime, LocalTime endTime,
                                             int slotMinutes) {
        if (professional == null || location == null || startDate == null || endDate == null
                || pattern == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("All availability fields are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (slotMinutes <= 0) {
            throw new IllegalArgumentException("Slot length must be positive");
        }

        long slotsPerDay = Duration.between(startTime, endTime).toMinutes() / slotMinutes;
        List<TimeSlot> created = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!pattern.includes(date)) {
                continue;
            }
            for (int i = 0; i < slotsPerDay; i++) {
                LocalTime from = startTime.plusMinutes((long) i * slotMinutes);
                created.add(new TimeSlot(professional, location, date, from, from.plusMinutes(slotMinutes)));
            }
        }

        List<TimeSlot> conflicts = new ArrayList<>();
        for (TimeSlot candidate : created) {
            for (TimeSlot existing : slots) {
                if (candidate.overlaps(existing)) {
                    conflicts.add(existing);
                }
            }
        }
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Availability overlaps existing slots: " + conflicts);
        }

        slots.addAll(created);
        return created;
    }

    
    public List<TimeSlot> searchAvailability(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = SearchCriteria.any();
        }
        List<TimeSlot> results = new ArrayList<>();
        for (TimeSlot slot : slots) {
            if (slot.isOpen() && matches(slot, criteria)) {
                results.add(slot);
            }
        }
        results.sort(Comparator.comparing(TimeSlot::getDate).thenComparing(TimeSlot::getStartTime));
        return results;
    }

    private boolean matches(TimeSlot slot, SearchCriteria criteria) {
        if (criteria.getProfessional() != null && slot.getProfessional() != criteria.getProfessional()) {
            return false;
        }
        if (criteria.getSpecialty() != null && !slot.getProfessional().hasSpecialty(criteria.getSpecialty())) {
            return false;
        }
        if (criteria.getFromDate() != null && slot.getDate().isBefore(criteria.getFromDate())) {
            return false;
        }
        if (criteria.getToDate() != null && slot.getDate().isAfter(criteria.getToDate())) {
            return false;
        }
        return true;
    }

    public void addSlot(TimeSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("Slot is required");
        }
        for (TimeSlot existing : slots) {
            if (slot.overlaps(existing)) {
                throw new IllegalStateException("Slot overlaps existing slot: " + existing);
            }
        }
        slots.add(slot);
    }

    public List<TimeSlot> getAllSlots() {
        return Collections.unmodifiableList(slots);
    }
}