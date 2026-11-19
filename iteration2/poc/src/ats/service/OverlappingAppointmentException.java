package ats.service;

import ats.domain.Appointment;

/** UC1 E5b: the Patient already has an Appointment at an overlapping time. */
public class OverlappingAppointmentException extends RuntimeException {
    private final Appointment conflict;

    public OverlappingAppointmentException(Appointment conflict) {
        super("The patient already has an appointment at an overlapping time.");
        this.conflict = conflict;
    }

    public Appointment getConflict() { return conflict; }
}
