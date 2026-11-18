package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public class TimeSlot {

    private final HealthcareProfessional professional;
    private final ClinicLocation location;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private SlotStatus status = SlotStatus.OPEN;   // not final: changes when booked

    public TimeSlot(HealthcareProfessional professional, ClinicLocation location,
                    LocalDate date, LocalTime startTime, LocalTime endTime) {

        if (professional == null || location == null || date == null
                || startTime == null || endTime == null) {
            throw new IllegalArgumentException("All time slot fields are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        this.professional = professional;
        this.location = location;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    
    public boolean overlaps(TimeSlot other) {
        return professional.equals(other.professional) 
                && other.date.equals(date) 
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    
    public boolean isOpen() {
        return (status == SlotStatus.OPEN); 
    }

    void book() {
        if (status != SlotStatus.OPEN)
            throw new IllegalStateException("already booked");
        else
            status = SlotStatus.BOOKED;

    }

    void release() {
        status = SlotStatus.OPEN;
    }

    public HealthcareProfessional getProfessional() {
        return professional;
    }

    public ClinicLocation getLocation() {
        return location;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public SlotStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return professional + " | " + date + " " + startTime + "-" + endTime
                + " | " + location + " | " + status;
    }
}